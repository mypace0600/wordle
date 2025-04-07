package com.wordle.quiz.controller;

import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private static final String ATTEMPTS_KEY_PREFIX = "attempts:user:%d:quiz:%d";
    private final RedisTemplate<String, Object> redisTemplate;
    private final QuizService quizService;

    /**
     * 새로운 퀴즈 시작
     */
    @PostMapping("/start")
    public ResponseEntity<QuizStartResponse> startQuiz(@AuthenticationPrincipal String userEmail) {
        log.info("Starting quiz for user: {}", userEmail);
        QuizStartResponse quiz = quizService.startQuiz(userEmail);
        return ResponseEntity.ok(quiz);
    }

    /**
     * 퀴즈 정보 조회
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizStartResponse> getQuizDetails(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Fetching quiz details for user: {}, quizId: {}", userEmail, quizId);
        QuizStartResponse quiz = quizService.getQuizDetails(userEmail, quizId);
        return ResponseEntity.ok(quiz);
    }

    /**
     * 퀴즈 정답 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitAnswer(
            @AuthenticationPrincipal String userEmail,
            @RequestBody QuizAnswerRequest request) {
        log.info("Submitting answer for user: {}", userEmail);
        QuizResultResponse result = quizService.submitAnswer(userEmail, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 시도 횟수 초기화
     */
    @PostMapping("/reset/{quizId}")
    public ResponseEntity<Void> resetAttempts(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Resetting attempts for user: {}, quizId: {}", userEmail, quizId);
        quizService.resetAttempts(userEmail, quizId);
        return ResponseEntity.ok().build();
    }
}