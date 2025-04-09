package com.wordle.quiz.controller;

import com.wordle.quiz.config.JwtUtil;
import com.wordle.quiz.dto.ApiResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(@CookieValue(value = "token", required = false) String token) {
        jwtUtil.validateOrThrow(token); // 유효성 검사 실패 시 IllegalArgumentException 발생

        String email = jwtUtil.extractEmail(token);
        List<String> roles = jwtUtil.extractRoles(token);

        Map<String, Object> response = Map.of("email", email, "roles", roles);
        return ResponseEntity.ok(new ApiResponse<>(response, "인증 확인 성공", 200));
    }

    @GetMapping("/admin-check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkAdmin(@CookieValue(value = "token", required = false) String token) {
        jwtUtil.validateOrThrow(token);
        List<String> roles = jwtUtil.extractRoles(token);

        if (!roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("관리자만 접근할 수 있습니다.");
        }

        return ResponseEntity.ok(new ApiResponse<>(Map.of("isAdmin", true), "관리자 확인 성공", 200));
    }

    @PostMapping("/custom-logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(new ApiResponse<>("Logged out", "로그아웃 성공", 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(@CookieValue(name = "token", required = false) String token) {
        jwtUtil.validateOrThrow(token);
        String email = jwtUtil.extractEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Map<String, Object> response = new HashMap<>();
        response.put("email", user.getEmail());
        response.put("isAdmin", user.isAdmin());
        response.put("score", user.getScore());

        return ResponseEntity.ok(new ApiResponse<>(response, "유저 정보 조회 성공", 200));
    }
}
