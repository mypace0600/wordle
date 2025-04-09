package com.wordle.quiz.controller;

import com.wordle.quiz.config.CustomOAuth2User;
import com.wordle.quiz.dto.ApiResponse;
import com.wordle.quiz.dto.RankResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(@AuthenticationPrincipal CustomOAuth2User user) {
        if (user == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        UserResponse userInfo = userService.getUserInfo(user.getName());
        if (userInfo == null) {
            throw new IllegalArgumentException("유저 정보를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(new ApiResponse<>(userInfo, "유저 정보 조회 성공", 200));
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<RankResponse>>> getRanking(@AuthenticationPrincipal String email) {
        if (email == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        List<RankResponse> rankingList = userService.getRankingList(email);
        return ResponseEntity.ok(new ApiResponse<>(rankingList, "랭킹 조회 성공", 200));
    }
}
