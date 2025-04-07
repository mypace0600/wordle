package com.wordle.quiz.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.getTokenFromRequest(request);
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