package com.wordle.quiz.controller;

import com.wordle.quiz.config.CustomOAuth2User;
import com.wordle.quiz.dto.RankingResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;



    @GetMapping
    public ResponseEntity<UserResponse> getUserInfo(@AuthenticationPrincipal CustomOAuth2User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = user.getName();
        UserResponse userInfo = userService.getUserInfo(email);
        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(userInfo);
    }


    @GetMapping("/ranking")
    public ResponseEntity<List<RankingResponse>> getRanking(@AuthenticationPrincipal String email) {
        if (email == null) {
            logger.warn("No authenticated user found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<RankingResponse> rankingList = userService.getRankingList(email);
        return ResponseEntity.ok(rankingList);
    }
}