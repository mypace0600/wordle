package com.wordle.quiz.service;

import com.wordle.quiz.dto.*;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_ATTEMPTS = 3;
    private static final String ATTEMPTS_KEY_PREFIX = "attempts:user:%d:quiz:%d";
    private static final String PREVIOUS_ATTEMPTS_KEY_PREFIX = "previous:user:%d:quiz:%d";

    // 캐싱된 미해결 퀴즈 조회
    public List<Quiz> findUnsolvedQuizzesByUser(String userEmail) {
        String cacheKey = "unsolved_quizzes:" + userEmail;
        List<Quiz> cached = (List<Quiz>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Returning cached unsolved quizzes for user: {}", userEmail);
            return cached;
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Quiz> unsolved = quizRepository.findUnsolvedQuizzesByUser(user);
        redisTemplate.opsForValue().set(cacheKey, unsolved, 10, TimeUnit.MINUTES);
        log.info("Cached unsolved quizzes for user: {}", userEmail);
        return unsolved;
    }

    // 퀴즈 초기화 및 응답 생성 공통 메서드
    private QuizStartResponse initializeAndBuildResponse(String userId, Quiz quiz) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserQuiz attempt = initializeUserQuiz(user, quiz, userId);
        String attemptsKey = getAttemptsKey(user.getId(), quiz.getId());
        redisTemplate.opsForValue().setIfAbsent(attemptsKey, 0);
        return buildQuizStartResponse(user, quiz);
    }

    // 새로운 퀴즈 시작
    public QuizStartResponse startQuiz(String userId) {
        List<Quiz> unsolvedQuizzes = findUnsolvedQuizzesByUser(userId);
        Quiz selectedQuiz = selectRandomQuiz(unsolvedQuizzes, quizRepository.findAll());
        return initializeAndBuildResponse(userId, selectedQuiz);
    }

    // 특정 퀴즈 정보 조회
    public QuizStartResponse getQuizDetails(String userId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + quizId));
        return initializeAndBuildResponse(userId, quiz);
    }

    // 랜덤 퀴즈 선택
    private Quiz selectRandomQuiz(List<Quiz> unsolvedQuizzes, List<Quiz> allQuizzes) {
        if (unsolvedQuizzes.isEmpty()) {
            log.warn("No unsolved quizzes found, selecting from all quizzes");
            if (allQuizzes.isEmpty()) {
                throw new IllegalStateException("No quizzes available in the system.");
            }
            return allQuizzes.get(new Random().nextInt(allQuizzes.size()));
        }
        return unsolvedQuizzes.get(new Random().nextInt(unsolvedQuizzes.size()));
    }

    // UserQuiz 초기화
    private UserQuiz initializeUserQuiz(User user, Quiz quiz, String userId) {
        return userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                .orElseGet(() -> {
                    UserQuiz newAttempt = new UserQuiz(user, quiz);
                    log.info("Creating new UserQuiz for user {} and quiz {}", userId, quiz.getId());
                    return userQuizRepository.save(newAttempt);
                });
    }

    // 퀴즈 시작 응답 생성
    private QuizStartResponse buildQuizStartResponse(User user, Quiz quiz) {
        List<Quiz> unsolvedQuizzes = findUnsolvedQuizzesByUser(user.getEmail());
        Long nextQuizId = getNextQuizId(quiz, unsolvedQuizzes);

        return new QuizStartResponse(
                quiz.getId(),
                quiz.getAnswer().length(),
                MAX_ATTEMPTS,
                nextQuizId
        );
    }

    // 다음 퀴즈 ID 계산
    private Long getNextQuizId(Quiz currentQuiz, List<Quiz> unsolvedQuizzes) {
        int index = unsolvedQuizzes.indexOf(currentQuiz);
        return (index >= 0 && index < unsolvedQuizzes.size() - 1) ? unsolvedQuizzes.get(index + 1).getId() : null;
    }

    // 퀴즈 정답 제출
    public QuizResultResponse submitAnswer(String userId, QuizAnswerRequest request) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserQuiz attempt = userQuizRepository.findByUserIdAndQuizId(user.getId(), request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found"));

        if (attempt.isSolved()) {
            throw new IllegalStateException("Quiz already solved");
        }

        int attempts = getAttempts(user.getId(), request.getQuizId());
        log.info("Current attempts for user {} on quiz {}: {}", userId, request.getQuizId(), attempts);
        if (attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("No attempts remaining");
        }

        String guess = request.getAnswer().toUpperCase();
        if (guess.length() != attempt.getQuiz().getAnswer().length()) {
            throw new IllegalArgumentException("Guess length must match quiz word length");
        }

        incrementAttempts(user.getId(), request.getQuizId());
        attempts++;
        log.info("Attempts incremented to: {}", attempts);

        List<QuizResultResponse.LetterResult> feedback = generateFeedback(attempt.getQuiz().getAnswer(), guess);
        boolean isCorrect = feedback.stream().allMatch(fr -> "correct".equals(fr.getStatus()));

        savePreviousAttempt(user.getId(), request.getQuizId(), feedback);

        if (isCorrect) {
            attempt.setSolved(true);
            user.setScore(user.getScore() + 10);
            userRepository.save(user);
            redisTemplate.delete(getAttemptsKey(user.getId(), request.getQuizId()));
            redisTemplate.delete(getPreviousAttemptsKey(user.getId(), request.getQuizId()));
            log.info("Quiz solved, Redis data cleared for user {} and quiz {}", userId, request.getQuizId());
        }

        userQuizRepository.save(attempt);

        return new QuizResultResponse(isCorrect, isCorrect ? 10 : 0, MAX_ATTEMPTS - attempts, feedback);
    }

    // Redis에서 시도 횟수 증가
    private void incrementAttempts(Long userId, Long quizId) {
        String key = getAttemptsKey(userId, quizId);
        redisTemplate.opsForValue().increment(key, 1);
    }

    // 이전 시도 기록 저장
    private void savePreviousAttempt(Long userId, Long quizId, List<QuizResultResponse.LetterResult> feedback) {
        String key = getPreviousAttemptsKey(userId, quizId);
        List<List<QuizResultResponse.LetterResult>> previousAttempts = getPreviousAttempts(userId, quizId);
        previousAttempts.add(feedback);
        redisTemplate.opsForValue().set(key, previousAttempts);
    }

    // 이전 시도 기록 조회
    @SuppressWarnings("unchecked")
    private List<List<QuizResultResponse.LetterResult>> getPreviousAttempts(Long userId, Long quizId) {
        String key = getPreviousAttemptsKey(userId, quizId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? (List<List<QuizResultResponse.LetterResult>>) value : new ArrayList<>();
    }

    // Redis에서 시도 횟수 조회
    private int getAttempts(Long userId, Long quizId) {
        String key = getAttemptsKey(userId, quizId);
        Object value = redisTemplate.opsForValue().get(key);
        try {
            return (value != null) ? Integer.parseInt(value.toString()) : 0;
        } catch (NumberFormatException e) {
            log.warn("Invalid attempts value in Redis for key {}: {}", key, value);
            return 0;
        }
    }

    // Redis 키 생성
    private String getAttemptsKey(Long userId, Long quizId) {
        return String.format(ATTEMPTS_KEY_PREFIX, userId, quizId);
    }

    private String getPreviousAttemptsKey(Long userId, Long quizId) {
        return String.format(PREVIOUS_ATTEMPTS_KEY_PREFIX, userId, quizId);
    }

    // 퀴즈 생성 (어드민)
    public QuizResponse createQuiz(@Valid QuizRequest request) {
        log.info("Creating quiz with answer: {}", request.getAnswer());
        if (quizRepository.existsByAnswer(request.getAnswer())) {
            log.warn("Duplicate quiz answer detected: {}", request.getAnswer());
            throw new IllegalArgumentException("이미 등록된 퀴즈입니다.");
        }

        Quiz quiz = new Quiz();
        quiz.setAnswer(request.getAnswer());
        quiz.setHint(request.getHint());
        Quiz savedQuiz = quizRepository.save(quiz);

        return QuizResponse.builder()
                .id(savedQuiz.getId())
                .answer(savedQuiz.getAnswer())
                .hint(savedQuiz.getHint())
                .build();
    }

    // 퀴즈 수정 (어드민)
    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        log.info("Updating quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quiz.setAnswer(request.getAnswer());
        quiz.setHint(request.getHint());
        Quiz updatedQuiz = quizRepository.save(quiz);

        return QuizResponse.builder()
                .id(updatedQuiz.getId())
                .answer(updatedQuiz.getAnswer())
                .hint(updatedQuiz.getHint())
                .build();
    }

    // 퀴즈 삭제 (어드민)
    public void deleteQuiz(Long id) {
        log.info("Deleting quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quizRepository.delete(quiz);
    }

    // 퀴즈 목록 조회 (어드민)
    public Page<QuizResponse> getQuizList(Pageable pageable,String keyword) {
        Page<Quiz> quizPage;
        if (keyword != null && !keyword.isBlank()) {
            quizPage = quizRepository.findByAnswerContainingIgnoreCase(keyword, pageable);
        } else {
            quizPage = quizRepository.findAll(pageable);
        }
        return quizPage.map(quiz -> new QuizResponse(quiz.getId(), quiz.getAnswer(), quiz.getHint()));
    }

    // Wordle 피드백 생성
    private List<QuizResultResponse.LetterResult> generateFeedback(String answer, String guess) {
        List<QuizResultResponse.LetterResult> feedback = new ArrayList<>();
        boolean[] used = new boolean[answer.length()];

        String answerUpper = answer.toUpperCase();
        String guessUpper = guess.toUpperCase();

        for (int i = 0; i < answerUpper.length(); i++) {
            char g = guessUpper.charAt(i);
            if (g == answerUpper.charAt(i)) {
                feedback.add(new QuizResultResponse.LetterResult(guess.charAt(i), "correct"));
                used[i] = true;
            } else {
                feedback.add(null);
            }
        }

        for (int i = 0; i < answerUpper.length(); i++) {
            if (feedback.get(i) == null) {
                char g = guessUpper.charAt(i);
                boolean misplaced = false;
                for (int j = 0; j < answerUpper.length(); j++) {
                    if (!used[j] && g == answerUpper.charAt(j)) {
                        feedback.set(i, new QuizResultResponse.LetterResult(guess.charAt(i), "misplaced"));
                        used[j] = true;
                        misplaced = true;
                        break;
                    }
                }
                if (!misplaced) {
                    feedback.set(i, new QuizResultResponse.LetterResult(guess.charAt(i), "incorrect"));
                }
            }
        }

        return feedback;
    }

    // 시도 횟수 초기화
    public void resetAttempts(String userEmail, Long quizId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String attemptsKey = String.format(ATTEMPTS_KEY_PREFIX, user.getId(), quizId);
        redisTemplate.opsForValue().set(attemptsKey, 0);
    }
}