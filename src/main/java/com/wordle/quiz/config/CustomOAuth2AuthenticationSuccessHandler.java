package com.wordle.quiz.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;

    public CustomOAuth2AuthenticationSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String email = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        String newToken = jwtUtil.generateToken(email, roles);
        response.setContentType("application/json");
        response.getWriter().write("{\"token\": \"" + newToken + "\"}");


        String token = jwtUtil.generateToken(email, roles);
        log.info("Generated JWT token for user: {}, roles: {}, token: {}", email, roles, token);
        // /callback으로 리다이렉트
        String redirectUrl = "http://localhost:5173/callback?token=" + token + "&email=" + email;
        response.sendRedirect(redirectUrl);
    }
}