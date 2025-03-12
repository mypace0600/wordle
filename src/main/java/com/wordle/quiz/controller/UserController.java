package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.RankingResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<UserResponse> getUserInfo(@RequestHeader("Authorization") String token) {
        String cleanedToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (!jwtUtil.validateToken(cleanedToken)) {
            logger.warn("Invalid token provided: {}", cleanedToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtUtil.extractEmail(cleanedToken);
        UserResponse user = userService.getUserInfo(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<RankingResponse>> getRanking(@RequestHeader("Authorization") String token) {
        String cleanedToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (!jwtUtil.validateToken(cleanedToken)) {
            logger.warn("Invalid token provided: {}", cleanedToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = jwtUtil.extractEmail(cleanedToken);
        List<RankingResponse> rankingList = userService.getRankingList(userId);
        return ResponseEntity.ok(rankingList);
    }
}