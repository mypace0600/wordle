package com.wordle.quiz.controller;

import com.wordle.quiz.dto.*;
import com.wordle.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 퀴즈 시작
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<QuizStartResponse>> startQuiz(
            @AuthenticationPrincipal String userEmail) {
        log.info("Starting quiz for user: {}", userEmail);
        QuizStartResponse quiz = quizService.startQuiz(userEmail);
        return ResponseEntity.ok(
                new ApiResponse<>(quiz, "퀴즈가 시작되었습니다.", HttpStatus.OK.value())
        );
    }

    /**
     * 퀴즈 상세 조회
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<ApiResponse<QuizStartResponse>> getQuizDetails(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Fetching quiz details for user: {}, quizId: {}", userEmail, quizId);
        QuizStartResponse quiz = quizService.getQuizDetails(userEmail, quizId);
        return ResponseEntity.ok(
                new ApiResponse<>(quiz, "퀴즈 정보를 불러왔습니다.", HttpStatus.OK.value())
        );
    }

    /**
     * 정답 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitAnswer(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody QuizAnswerRequest request) {
        log.info("Submitting answer for user: {}", userEmail);
        QuizResultResponse result = quizService.submitAnswer(userEmail, request);
        return ResponseEntity.ok(
                new ApiResponse<>(result, "정답 제출 결과입니다.", HttpStatus.OK.value())
        );
    }

    /**
     * 퀴즈 재도전 (시도 횟수 초기화)
     */
    @PostMapping("/reset/{quizId}")
    public ResponseEntity<ApiResponse<Void>> resetAttempts(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Resetting attempts for user: {}, quizId: {}", userEmail, quizId);
        quizService.resetAttempts(userEmail, quizId);
        return ResponseEntity.ok(
                new ApiResponse<>(null, "시도 횟수가 초기화되었습니다.", HttpStatus.OK.value())
        );
    }
}
