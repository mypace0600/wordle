package com.wordle.quiz.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationTime;

    public JwtUtil(@Value("${jwt.secret}") String secretKey,
                   @Value("${jwt.expiration}") long expirationTime) {
        if (secretKey.getBytes().length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.expirationTime = expirationTime;
    }

    public String generateToken(String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // extractRoles 타입 안전하게
    public List<String> extractRoles(String token) {
        Object rolesObject = extractAllClaims(token).get("roles");
        if (rolesObject instanceof List<?> rawList) {
            return rawList.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of(); // roles가 없는 경우 빈 리스트 반환
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    public void validateOrThrow(String token) {
        if (token == null) {
            throw new IllegalArgumentException("토큰이 존재하지 않습니다.");
        }

        Claims claims = extractAllClaims(token);
        if (claims.getSubject() == null || claims.getExpiration().before(new Date())) {
            throw new IllegalArgumentException("토큰 정보가 유효하지 않습니다.");
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2. 쿠키
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
