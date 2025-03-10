package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.GoogleAuthRequest;
import com.wordle.quiz.dto.RankingResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 구글 로그인 처리
     */
    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request) {
        String token = userService.authenticateWithGoogle(request);
        System.out.println("🔐 toke : "+token);
        return ResponseEntity.ok(Collections.singletonMap("token", token));
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @GetMapping
    public ResponseEntity<UserResponse> getUserInfo(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractEmail(token);
        UserResponse user = userService.getUserInfo(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * 랭킹 조회 (상위 10명 & 내 랭킹 포함)
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingResponse>> getRanking(@RequestHeader("Authorization") String token) {
        String userId = jwtUtil.extractEmail(token);
        List<RankingResponse> rankingList = userService.getRankingList(userId);
        return ResponseEntity.ok(rankingList);
    }
}
