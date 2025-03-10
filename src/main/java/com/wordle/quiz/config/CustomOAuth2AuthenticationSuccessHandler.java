package com.wordle.quiz.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;

    public CustomOAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String username = oauthToken.getPrincipal().getAttribute("email"); // 예시, 프로필 정보에 맞게 조정 필요

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(username);
        // System.out.println("@@@@@ token : "+token);
        String email = jwtUtil.extractEmail(token);
        // System.out.println("@@@@@ email : "+email);

        response.setHeader("Authorization", "Bearer " + token);
        // 리다이렉트 경로 지정
        getRedirectStrategy().sendRedirect(request, response, "/home"); // 예시로 /home 경로로 리다이렉트

    }
}

