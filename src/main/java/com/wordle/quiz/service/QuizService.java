package com.wordle.quiz.service;

import com.wordle.quiz.config.RedisUserStateService;
import com.wordle.quiz.dto.LetterResult;
import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.enums.LetterStatus;
import com.wordle.quiz.exception.NoAvailableQuizException;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserQuizRepository userQuizRepository;
    private final RedisUserStateService redisUserStateService;
    private static final int MAX_ATTEMPTS = 3;
    private static final int SCORE_PER_CORRECT = 10;

    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);

        List<Quiz> cached = redisUserStateService.getCachedUnsolvedQuizzes(email);
        if (cached.isEmpty()) {
            List<Quiz> all = quizRepository.findAll();
            if (all.isEmpty()) {
                throw new NoAvailableQuizException("시스템에 등록된 퀴즈가 없습니다."); // DB 자체가 비어있는 상황
            }

            Set<Long> solvedIds = user.getQuizAttempts().stream()
                    .map(a -> a.getQuiz().getId())
                    .collect(Collectors.toSet());

            List<Quiz> unsolved = all.stream()
                    .filter(q -> !solvedIds.contains(q.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(unsolved);
            redisUserStateService.cacheUnsolvedQuizzes(email, unsolved);
            cached = new ArrayList<>(unsolved);
        }

        if (cached.isEmpty()) {
            List<Quiz> all = quizRepository.findAll();
            if (all.isEmpty()) throw new NoAvailableQuizException("시스템에 등록된 퀴즈가 없습니다.");

            // ✅ 유저가 푼 퀴즈 ID 가져오기
            List<Long> solvedIds = userQuizRepository.findSolvedQuizIdsByUser(user.getId());

            List<Quiz> unsolved = all.stream()
                    .filter(q -> !solvedIds.contains(q.getId()))
                    .collect(Collectors.toList());

            Collections.shuffle(unsolved);
            redisUserStateService.cacheUnsolvedQuizzes(email, unsolved);
            cached = new ArrayList<>(unsolved);
        }

        Quiz quiz = cached.remove(0);
        Quiz nextQuiz = !cached.isEmpty() ? cached.get(0) : null;

        redisUserStateService.cacheUnsolvedQuizzes(email, cached);

        return QuizStartResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(nextQuiz != null ? nextQuiz.getId() : null)
                .build();
    }


    public QuizStartResponse getQuizDetails(String email, Long quizId) {
        Quiz quiz = getQuizById(quizId);

        return QuizStartResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(null) // 그냥 디테일이니까 nextQuiz는 null 처리
                .build();
    }


    public QuizResultResponse submitAnswer(String email, QuizAnswerRequest request) {
        User user = getUser(email);
        Quiz quiz = getQuizById(request.getQuizId());

        int attempts = redisUserStateService.getAttempts(user.getEmail(), quiz.getId());
        int hearts = redisUserStateService.getHearts(user.getEmail());

        if (attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("최대 시도 횟수를 초과했습니다.");
        }
        if (hearts <= 0) {
            throw new IllegalStateException("하트가 부족합니다.");
        }

        redisUserStateService.incrementAttempts(user.getEmail(), quiz.getId());

        String answer = quiz.getAnswer().toLowerCase();
        String submitted = request.getAnswer().toLowerCase();
        List<Character> submittedChars = submitted.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        List<LetterResult> feedback = generateFeedback(answer, submittedChars);
        boolean isCorrect = feedback.stream().allMatch(r -> r.getStatus() == LetterStatus.CORRECT);

        if (isCorrect) {
            redisUserStateService.resetAttempts(user.getEmail(), quiz.getId());
            redisUserStateService.deleteUnsolvedCache(email);
            user.setScore(user.getScore() + SCORE_PER_CORRECT);
            userRepository.save(user);

            // ✅ UserQuiz 기록 저장
            UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                    .orElse(new UserQuiz(user, quiz, 0, false, null));

            userQuiz.setSolved(true);
            userQuiz.setLastTriedAt(LocalDateTime.now());
            userQuizRepository.save(userQuiz);
        } else {
            redisUserStateService.decrementHearts(user.getEmail());

            // ✅ 오답 기록도 저장
            UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                    .orElse(new UserQuiz(user, quiz, 0, false, null));

            userQuiz.setAttempts(userQuiz.getAttempts() + 1);
            userQuiz.setLastTriedAt(LocalDateTime.now());
            userQuizRepository.save(userQuiz);
        }


        return new QuizResultResponse(
                isCorrect,
                user.getScore(),
                MAX_ATTEMPTS - redisUserStateService.getAttempts(user.getEmail(), quiz.getId()),
                feedback
        );
    }


    // ====== Feedback 생성 ======

    private List<LetterResult> generateFeedback(String answer, List<Character> submitted) {
        List<LetterResult> result = new ArrayList<>();
        boolean[] used = new boolean[answer.length()];

        // First pass: 정확한 위치
        for (int i = 0; i < submitted.size(); i++) {
            char ch = submitted.get(i);
            if (i < answer.length() && ch == answer.charAt(i)) {
                used[i] = true;
                result.add(new LetterResult(ch, LetterStatus.CORRECT));
            } else {
                result.add(null); // 임시 placeholder
            }
        }

        // Second pass: 다른 위치 존재 여부
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

    // ====== Redis 유틸 ======




    // ====== 기타 ======

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
    }

}
