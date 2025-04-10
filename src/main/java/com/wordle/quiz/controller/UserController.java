package com.wordle.quiz.controller;

import com.wordle.quiz.dto.ApiResponse;
import com.wordle.quiz.dto.StatResponse;
import com.wordle.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/stat")
    public ResponseEntity<ApiResponse<StatResponse>> getUserStat(@AuthenticationPrincipal String userEmail) {
        if (userEmail == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        log.info("Getting stat for user: {}", userEmail);
        StatResponse stat = userService.getUserStatistics(userEmail);

        return ResponseEntity.ok(new ApiResponse<>(stat, "유저 통계 조회 성공", 200));
    }
}
