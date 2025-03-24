package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.repository.UserRepository;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {


    private static final String ATTEMPTS_KEY_PREFIX = "attempts:user:%d:quiz:%d";
    private final RedisTemplate<String, Object> redisTemplate;

    private final QuizService quizService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 새로운 퀴즈 시작
     */
    @PostMapping("/start")
    public ResponseEntity<QuizStartResponse> startQuiz(@RequestHeader("Authorization") String token) {
        token = extractTokenFromHeader(token);
        String userEmail = jwtUtil.extractEmail(token);
        QuizStartResponse quiz = quizService.startQuiz(userEmail);
        return ResponseEntity.ok(quiz);
    }

    /**
     * 퀴즈 정보 조회 (추가)
     */
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizStartResponse> getQuizDetails(
            @RequestHeader("Authorization") String token,
            @PathVariable Long quizId) {
        token = extractTokenFromHeader(token);
        String userEmail = jwtUtil.extractEmail(token);
        QuizStartResponse quiz = quizService.getQuizDetails(userEmail, quizId);
        return ResponseEntity.ok(quiz);
    }

    /**
     * 퀴즈 정답 제출
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultResponse> submitAnswer(
            @RequestHeader("Authorization") String token,
            @RequestBody QuizAnswerRequest request) {
        token = extractTokenFromHeader(token);
        String userEmail = jwtUtil.extractEmail(token);
        QuizResultResponse result = quizService.submitAnswer(userEmail, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset/{quizId}")
    public ResponseEntity<Void> resetAttempts(
            @RequestHeader("Authorization") String token,
            @PathVariable Long quizId) {
        token = extractTokenFromHeader(token);
        String userEmail = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        String attemptsKey = String.format(ATTEMPTS_KEY_PREFIX, user.getId(), quizId);
        redisTemplate.opsForValue().set(attemptsKey, 0);
        return ResponseEntity.ok().build();
    }

    // 토큰 추출 헬퍼 메서드
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return authorizationHeader.substring(7); // "Bearer " 제거
    }
}