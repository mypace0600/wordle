package com.wordle.quiz.service;

import com.wordle.quiz.config.RedisUserStateService;
import com.wordle.quiz.dto.*;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.enums.LetterStatus;
import com.wordle.quiz.exception.NoAvailableQuizException;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserQuizRepository userQuizRepository;
    private final RedisUserStateService redisUserStateService;

    private static final int MAX_ATTEMPTS = 2;
    private static final int SCORE_PER_CORRECT = 10;

    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);
        List<Long> solvedQuizIds = normalizeSolvedIds(userQuizRepository.findSolvedQuizIdsByUser(user.getId()));

        List<Quiz> unsolvedQuizzes = quizRepository.findUnsolvedQuizzesRandomlyNative(solvedQuizIds, 2);
        if (unsolvedQuizzes.isEmpty()) {
            throw new NoAvailableQuizException("남은 퀴즈가 없습니다.");
        }

        Quiz quiz = unsolvedQuizzes.get(0);
        Quiz nextQuiz = unsolvedQuizzes.size() > 1 ? unsolvedQuizzes.get(1) : null;

        return QuizStartResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(nextQuiz != null ? nextQuiz.getId() : null)
                .build();
    }

    public QuizDetailResponse getQuizDetails(Long quizId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(()->new IllegalArgumentException("사용자가 없습니다."));
        // 이미 푼 퀴즈인지 확인
        boolean alreadySolved = userQuizRepository.existsByUserIdAndQuizId(user.getId(), quizId);
        if (alreadySolved) {
            throw new IllegalStateException("이미 푼 퀴즈에 접근할 수 없습니다.");
        }
        Quiz quiz = getQuizById(quizId);
        Long nextQuizId = getNextQuizId(userEmail, quizId);
        log.info("@@@@@@@@@@ nextQuizId : {}", nextQuizId);

        return QuizDetailResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(nextQuizId)
                .answer(quiz.getAnswer())
                .hint(quiz.getHint())
                .build();
    }

    public Long getNextQuizId(String userEmail, Long currentQuizId) {
        User user = getUser(userEmail);
        List<Long> solvedIds = userQuizRepository.findSolvedQuizIdsByUser(user.getId());
        solvedIds.add(currentQuizId); // 현재 퀴즈도 푼 것처럼 처리

        List<Quiz> unsolvedQuizzes = quizRepository.findUnsolvedQuizzesRandomlyNative(normalizeSolvedIds(solvedIds), 1);
        if (unsolvedQuizzes.isEmpty()) {
            return null;
        }

        Quiz q = unsolvedQuizzes.get(0);
        log.info("@@@@@@@@@@@@ quiz id:{} {}:{}", q.getId(), q.getAnswer(), q.getHint());
        return q.getId();
    }

    public QuizResultResponse submitAnswer(String email, QuizAnswerRequest request) {
        User user = getUser(email);
        Quiz quiz = getQuizById(request.getQuizId());

        int attempts = redisUserStateService.getAttempts(email, quiz.getId());
        int hearts = redisUserStateService.getHearts(email);
        log.info(">>> [Before Submit] email: {}, attempts: {}, hearts: {}", email, attempts, hearts);

        if (hearts <= 0) {
            throw new IllegalStateException("하트가 부족합니다.");
        }

        redisUserStateService.incrementAttempts(email, quiz.getId());

        String answer = quiz.getAnswer().toLowerCase();
        String submitted = request.getAnswer().toLowerCase();
        List<Character> submittedChars = submitted.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        List<LetterResult> feedback = generateFeedback(answer, submittedChars);
        boolean isCorrect = feedback.stream().allMatch(r -> r.getStatus() == LetterStatus.CORRECT);

        if (isCorrect) {
            redisUserStateService.resetAttempts(email, quiz.getId());
            redisUserStateService.deleteUnsolvedCache(email);
            user.setScore(user.getScore() + SCORE_PER_CORRECT);
            userRepository.save(user);

            saveOrUpdateUserQuiz(user, quiz, uq -> {
                uq.setSolved(true);
                uq.setLastTriedAt(LocalDateTime.now());
            });
        } else {
            saveOrUpdateUserQuiz(user, quiz, uq -> {
                uq.setAttempts(uq.getAttempts() + 1);
                uq.setLastTriedAt(LocalDateTime.now());
            });

            if (attempts >= MAX_ATTEMPTS) {
                redisUserStateService.decrementHearts(email);
                redisUserStateService.resetAttempts(email, quiz.getId());
            }
        }

        log.info(">>> [After Submit] email: {}, attempts: {}, hearts: {}", email,
                redisUserStateService.getAttempts(email, quiz.getId()),
                redisUserStateService.getHearts(email));

        return new QuizResultResponse(
                isCorrect,
                user.getScore(),
                MAX_ATTEMPTS - redisUserStateService.getAttempts(email, quiz.getId()) + 1,
                redisUserStateService.getHearts(email),
                feedback
        );
    }

    private List<LetterResult> generateFeedback(String answer, List<Character> submitted) {
        List<LetterResult> result = new ArrayList<>();
        boolean[] used = new boolean[answer.length()];

        for (int i = 0; i < submitted.size(); i++) {
            char ch = submitted.get(i);
            if (i < answer.length() && ch == answer.charAt(i)) {
                used[i] = true;
                result.add(new LetterResult(ch, LetterStatus.CORRECT));
            } else {
                result.add(null);
            }
        }

        for (int i = 0; i < submitted.size(); i++) {
            if (result.get(i) != null) continue;
            char ch = submitted.get(i);
            boolean found = false;
            for (int j = 0; j < answer.length(); j++) {
                if (!used[j] && answer.charAt(j) == ch) {
                    used[j] = true;
                    found = true;
                    break;
                }
            }
            result.set(i, new LetterResult(ch, found ? LetterStatus.MISPLACED : LetterStatus.INCORRECT));
        }

        return result;
    }

    private void saveOrUpdateUserQuiz(User user, Quiz quiz, Consumer<UserQuiz> updater) {
        UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                .orElse(new UserQuiz(user, quiz, 0, false, null));
        updater.accept(userQuiz);
        userQuizRepository.save(userQuiz);
    }

    private List<Long> normalizeSolvedIds(List<Long> ids) {
        return ids.isEmpty() ? Collections.singletonList(-1L) : ids;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));
    }

    private Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("해당 퀴즈를 찾을 수 없습니다."));
    }
}
