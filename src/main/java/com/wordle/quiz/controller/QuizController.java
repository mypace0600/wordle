package com.wordle.quiz.controller;

import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizRequest;
import com.wordle.quiz.dto.QuizResponse;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // 사용자용 엔드포인트
    @PostMapping("/start")
    public ResponseEntity<QuizStartResponse> startQuiz(@AuthenticationPrincipal String userEmail) {
        log.info("Starting quiz for user: {}", userEmail);
        QuizStartResponse quiz = quizService.startQuiz(userEmail);
        return ResponseEntity.ok(quiz);
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizStartResponse> getQuizDetails(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Fetching quiz details for user: {}, quizId: {}", userEmail, quizId);
        QuizStartResponse quiz = quizService.getQuizDetails(userEmail, quizId);
        return ResponseEntity.ok(quiz);
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitAnswer(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody QuizAnswerRequest request) {
        log.info("Submitting answer for user: {}", userEmail);
        QuizResultResponse result = quizService.submitAnswer(userEmail, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset/{quizId}")
    public ResponseEntity<Void> resetAttempts(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Resetting attempts for user: {}, quizId: {}", userEmail, quizId);
        quizService.resetAttempts(userEmail, quizId);
        return ResponseEntity.ok().build();
    }

    // 관리자용 엔드포인트
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<QuizResponse> createQuiz(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody QuizRequest request) {
        log.info("Admin {} creating quiz with answer: {}", userEmail, request.getAnswer());
        QuizResponse response = quizService.createQuiz(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{quizId}")
    public ResponseEntity<QuizResponse> updateQuiz(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId,
            @Valid @RequestBody QuizRequest request) {
        log.info("Admin {} updating quiz with id: {}", userEmail, quizId);
        QuizResponse response = quizService.updateQuiz(quizId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @AuthenticationPrincipal String userEmail,
            @PathVariable Long quizId) {
        log.info("Admin {} deleting quiz with id: {}", userEmail, quizId);
        quizService.deleteQuiz(quizId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<Page<QuizResponse>> getQuizList(
            @AuthenticationPrincipal String userEmail,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Admin {} fetching quiz list", userEmail);
        Page<QuizResponse> quizList = quizService.getQuizList(pageable);
        return ResponseEntity.ok(quizList);
    }
}