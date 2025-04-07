package com.wordle.quiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth provider.");
            return;
        }

        // 기본적으로 "ROLE_USER" 부여
        List<String> roles = List.of("ROLE_USER");
        if (email.equals("mypace0600@gmail.com")) {
            roles = List.of("ROLE_USER", "ROLE_ADMIN");
        }

        String token = jwtUtil.generateToken(email, roles);

        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true) // HTTPS 환경에서만 전송 (로컬 테스트 시 false 가능)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofHours(1))
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect("http://localhost:5173/home");
    }
}
