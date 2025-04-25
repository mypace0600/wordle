package com.wordle.quiz.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
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

        // ✅ 2번 - 이메일 값 확인 로그
        System.out.println("✅ OAuth 로그인 성공 - email: " + email);

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth provider.");
            return;
        }

        // 기본적으로 "ROLE_USER" 부여
        List<String> roles = List.of("ROLE_USER");
        if (email.equalsIgnoreCase("mypace0600@gmail.com")) {
            roles = List.of("ROLE_USER", "ROLE_ADMIN");
        }

        // ✅ 3번 - roles와 JWT 확인 로그
        System.out.println("✅ JWT 생성 - roles: " + roles);

        String token = jwtUtil.generateToken(email, roles);

        System.out.println("✅ JWT: " + token);

        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofHours(1))
                .build();

        System.out.println("@@@@@@@@ CustomOAuth2AuthenticationSuccessHandler cookie");
        System.out.println(cookie.toString());

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        System.out.println("✅ OAuth 로그인 성공 - email: " + email);
        System.out.println("✅ JWT 생성 - roles: " + roles);
        System.out.println("✅ JWT: " + token);
        response.sendRedirect("https://hyeonsu-side.com");
    }

}
