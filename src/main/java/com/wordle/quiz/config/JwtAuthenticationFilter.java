package com.wordle.quiz.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return
                // 기존
                path.equals("/health-check")
                        || path.equals("/api/auth/custom-logout")
                        || path.equals("/api/auth/check")
                        || path.equals("/api/auth/me")
                        // 여기에 추가
                        || path.startsWith("/oauth2/authorization/google")
                        || path.startsWith("/login/oauth2")
                        || path.equals("/login")
                ;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.getTokenFromRequest(request);

        // ✅ Authorization 헤더에 없으면, 쿠키에서 가져오기 시도
        if (token == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("Invalid or missing JWT token: {}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return;
        }
        if (token != null && jwtUtil.validateToken(token)) {
            String userId = jwtUtil.extractEmail(token);
            List<GrantedAuthority> authorities = jwtUtil.extractRoles(token)
                    .stream()
                    .map(role -> {
                        log.info("Extracted role: {}", role);
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toList());
            log.info("Setting authentication for user: {} with roles: {}", userId, authorities);

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(userId, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("Invalid or missing JWT token: {}", token);
        }
        filterChain.doFilter(request, response);
    }
}