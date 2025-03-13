package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        token = extractTokenFromHeader(token);
        log.info("@@@@@@ token :{}",token);
        String userId = jwtUtil.extractEmail(token);
        QuizStartResponse quiz = quizService.startQuiz(userId);
        return ResponseEntity.ok(quiz);
    }

    // 토큰 추출 헬퍼 메서드
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return authorizationHeader.substring(7); // "Bearer " 제거 (길이 7)
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
}
