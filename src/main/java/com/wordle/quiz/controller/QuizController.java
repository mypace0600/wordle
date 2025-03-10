package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.dto.QuizStatusResponse;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final JwtUtil jwtUtil;

    /**
     * 새로운 퀴즈 시작
     */
    @PostMapping("/start")
    public ResponseEntity<QuizStartResponse> startQuiz(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractEmail(token);
        QuizStartResponse quiz = quizService.startQuiz(userId);
        return ResponseEntity.ok(quiz);
    }

    /**
     * 퀴즈 정답 제출 (점수 지급, 시도 횟수 차감)
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitAnswer(
            @RequestHeader("Authorization") String token,
            @RequestBody QuizAnswerRequest request) {
        String userId = jwtUtil.extractEmail(token);
        QuizResultResponse result = quizService.submitAnswer(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 광고 시청 후 퀴즈 재시작
     */
    @PostMapping("/advertisement/viewed")
    public ResponseEntity<Void> viewAd(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractEmail(token);
        quizService.viewAdvertisement(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 퀴즈 진행 상태 조회
     */
    @GetMapping("/status/{quizId}")
    public ResponseEntity<QuizStatusResponse> getQuizStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String quizId) {
        String userId = jwtUtil.extractEmail(token);
        QuizStatusResponse status = quizService.getQuizStatus(userId, quizId);
        return ResponseEntity.ok(status);
    }
}
