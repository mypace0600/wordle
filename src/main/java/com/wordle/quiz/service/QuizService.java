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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserQuizRepository userQuizRepository;
    private final RedisUserStateService redisUserStateService;
    private static final int MAX_ATTEMPTS = 2; //0,1,2
    private static final int SCORE_PER_CORRECT = 10;

    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);

        List<Long> solvedQuizIds = userQuizRepository.findSolvedQuizIdsByUser(user.getId());
        if (solvedQuizIds.isEmpty()) {
            solvedQuizIds = Collections.singletonList(-1L); // 푼 게 없으면 전체 검색을 위해 더미 ID
        }

        List<Quiz> unsolvedQuizzes = quizRepository.findUnsolvedQuizzesRandomly(solvedQuizIds, PageRequest.of(0, 2));
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
        Quiz quiz = getQuizById(quizId);

        Long nextQuizId = getNextQuizId(userEmail,quizId);
        log.info("@@@@@@@@@@ nextQuizId : {}",nextQuizId);
        return QuizDetailResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(nextQuizId)
                .answer(quiz.getAnswer())
                .hint(quiz.getHint())
                .build();
    }

    public Long getNextQuizId(String userEmail, Long currentQuizId){
        User user = getUser(userEmail);
        List<Long> solvedIds = userQuizRepository.findSolvedQuizIdsByUser(user.getId());
        // 혹시 현재 퀴즈도 푼 상태가 아니었다면 제외 처리
        solvedIds.add(currentQuizId);

        List<Quiz> candidates = quizRepository.findUnsolvedQuizzesRandomly(solvedIds, PageRequest.of(0, 1));
        for(Quiz q : candidates){
            log.info("@@@@@@@@@@@@ quiz id:{} {}:{}",q.getId(),q.getAnswer(),q.getHint());
        }
        if (candidates.isEmpty()) {
            throw new NoAvailableQuizException("남은 퀴즈가 없습니다.");
        }

        return candidates.get(0).getId();
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

        log.info(">>> [After Submit] email: {}, attempts: {}, hearts: {}", email, redisUserStateService.getAttempts(email, quiz.getId()), hearts);


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

            // ✅ UserQuiz 기록 저장
            UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                    .orElse(new UserQuiz(user, quiz, 0, false, null));

            userQuiz.setSolved(true);
            userQuiz.setLastTriedAt(LocalDateTime.now());
            userQuizRepository.save(userQuiz);
        } else {
            // 오답 기록도 저장
            UserQuiz userQuiz = userQuizRepository.findByUserIdAndQuizId(user.getId(), quiz.getId())
                    .orElse(new UserQuiz(user, quiz, 0, false, null));

            userQuiz.setAttempts(userQuiz.getAttempts() + 1);
            userQuiz.setLastTriedAt(LocalDateTime.now());
            userQuizRepository.save(userQuiz);

            // 오답이고 최대 시도 횟수 이상일 경우 하트 차감 및 시도횟수 초기화
            if (attempts >= MAX_ATTEMPTS) {
                redisUserStateService.decrementHearts(email);
                redisUserStateService.resetAttempts(email, quiz.getId());
                log.info(">>> [After Decrement] email: {}, attempts: {}, hearts: {}", email, redisUserStateService.getAttempts(email, quiz.getId()), redisUserStateService.getHearts(email));
            }
        }

        return new QuizResultResponse(
                isCorrect,
                user.getScore(),
                MAX_ATTEMPTS-redisUserStateService.getAttempts(email, quiz.getId())+1,
                redisUserStateService.getHearts(email),
                feedback
        );
    }

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

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
    }

}
