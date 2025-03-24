package com.wordle.quiz.service;

import com.wordle.quiz.dto.*;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_ATTEMPTS = 3; // 최대 시도 횟수 3으로 변경
    private static final String ATTEMPTS_KEY_PREFIX = "attempts:user:%d:quiz:%d";
    private static final String PREVIOUS_ATTEMPTS_KEY_PREFIX = "previous:user:%d:quiz:%d";

    @Transactional
    public QuizStartResponse startQuiz(String userId) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Quiz> unsolvedQuizzes = quizRepository.findUnsolvedQuizzesByUser(user);
        log.info("Unsolved quizzes for user {}: {}", userId, unsolvedQuizzes.size());

        Quiz selectedQuiz = selectRandomQuiz(unsolvedQuizzes, quizRepository.findAll());
        UserQuiz attempt = initializeUserQuiz(user, selectedQuiz, userId);

        String attemptsKey = getAttemptsKey(user.getId(), selectedQuiz.getId());
        redisTemplate.opsForValue().setIfAbsent(attemptsKey, 0);

        return buildQuizStartResponse(user, selectedQuiz);
    }

    public QuizStartResponse getQuizDetails(String userId, Long quizId) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + quizId));

        UserQuiz attempt = initializeUserQuiz(user, quiz, userId);

        String attemptsKey = getAttemptsKey(user.getId(), quizId);
        redisTemplate.opsForValue().setIfAbsent(attemptsKey, 0);

        return buildQuizStartResponse(user, quiz);
    }

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

    private UserQuiz initializeUserQuiz(User user, Quiz quiz, String userId) {
        return userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                .orElseGet(() -> {
                    UserQuiz newAttempt = new UserQuiz(user, quiz);
                    log.info("Creating new UserQuiz for user {} and quiz {}", userId, quiz.getId());
                    return userQuizRepository.save(newAttempt);
                });
    }

    private QuizStartResponse buildQuizStartResponse(User user, Quiz quiz) {
        List<Quiz> unsolvedQuizzes = quizRepository.findUnsolvedQuizzesByUser(user);
        Long nextQuizId = getNextQuizId(quiz, unsolvedQuizzes);

        return new QuizStartResponse(
                quiz.getId(),
                quiz.getAnswer().length(),
                MAX_ATTEMPTS,
                nextQuizId
        );
    }

    private Long getNextQuizId(Quiz currentQuiz, List<Quiz> unsolvedQuizzes) {
        int index = unsolvedQuizzes.indexOf(currentQuiz);
        return (index >= 0 && index < unsolvedQuizzes.size() - 1) ? unsolvedQuizzes.get(index + 1).getId() : null;
    }

    public QuizResultResponse submitAnswer(String userId, QuizAnswerRequest request) {
        User user = userRepository.findByEmail(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserQuiz attempt = userQuizRepository.findByUserIdAndQuizId(user.getId(), request.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found"));

        if (attempt.isSolved()) {
            throw new IllegalStateException("Quiz already solved");
        }

        // Redis에서 시도 횟수 확인
        int attempts = getAttempts(user.getId(), request.getQuizId());
        log.info("Current attempts for user {} on quiz {}: {}", userId, request.getQuizId(), attempts);
        if (attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("No attempts remaining");
        }

        String guess = request.getAnswer().toUpperCase();
        if (guess.length() != attempt.getQuiz().getAnswer().length()) {
            throw new IllegalArgumentException("Guess length must match quiz word length");
        }

        // Redis 시도 횟수 증가
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

        return (value != null ? (Integer.parseInt(value.toString()) > 3 ? 0 :Integer.parseInt(value.toString()) ): 0);
    }

    // Redis 키 생성
    private String getAttemptsKey(Long userId, Long quizId) {
        return String.format(ATTEMPTS_KEY_PREFIX, userId, quizId);
    }

    private String getPreviousAttemptsKey(Long userId, Long quizId) {
        return String.format(PREVIOUS_ATTEMPTS_KEY_PREFIX, userId, quizId);
    }


    public QuizResponse createQuiz(QuizRequest request) {
        log.info("Creating quiz with answer: {}", request.getAnswer());

        // ANSWER 중복 체크
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

    // 어드민 퀴즈 수정
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

    // 어드민 퀴즈 삭제
    public void deleteQuiz(Long id) {
        log.info("Deleting quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quizRepository.delete(quiz);
    }

    // 어드민 퀴즈 목록 조회
    public Page<QuizResponse> getQuizList(Pageable pageable) {
        Page<Quiz> quizPage = quizRepository.findAll(pageable); // JPA로 페이징 조회
        return quizPage.map(quiz -> new QuizResponse(quiz.getId(), quiz.getAnswer(), quiz.getHint())); // 엔티티 -> DTO 변환
    }

    // Wordle 피드백 생성
    private List<QuizResultResponse.LetterResult> generateFeedback(String answer, String guess) {
        List<QuizResultResponse.LetterResult> feedback = new ArrayList<>();
        boolean[] used = new boolean[answer.length()];

        // 대소문자 구분 없이 비교하기 위해 모두 대문자로 변환
        String answerUpper = answer.toUpperCase();
        String guessUpper = guess.toUpperCase();

        // 먼저 "correct" (정확한 위치) 체크
        for (int i = 0; i < answerUpper.length(); i++) {
            char g = guessUpper.charAt(i);
            if (g == answerUpper.charAt(i)) {
                feedback.add(new QuizResultResponse.LetterResult(guess.charAt(i), "correct")); // 원래 guess 문자 유지
                used[i] = true;
            } else {
                feedback.add(null); // 임시로 null
            }
        }

        // "misplaced" (잘못된 위치)와 "incorrect" 체크
        for (int i = 0; i < answerUpper.length(); i++) {
            if (feedback.get(i) == null) {
                char g = guessUpper.charAt(i);
                boolean misplaced = false;
                for (int j = 0; j < answerUpper.length(); j++) {
                    if (!used[j] && g == answerUpper.charAt(j)) {
                        feedback.set(i, new QuizResultResponse.LetterResult(guess.charAt(i), "misplaced")); // 원래 guess 문자 유지
                        used[j] = true;
                        misplaced = true;
                        break;
                    }
                }
                if (!misplaced) {
                    feedback.set(i, new QuizResultResponse.LetterResult(guess.charAt(i), "incorrect")); // 원래 guess 문자 유지
                }
            }
        }

        return feedback;
    }
}
