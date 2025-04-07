package com.wordle.quiz.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${admin.email:mypace0600@gmail.com}")
    private String adminEmail;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .claim("roles", roles) // 권한 추가
                .claim("isAdmin", email.equals(adminEmail))
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            Claims body = claims.getBody();
            if (body.getExpiration().before(new Date())) {
                logger.error("Token is expired");
                return false;
            }
            if (body.getSubject() == null) {
                logger.error("Token subject is missing");
                return false;
            }
            logger.info("Token validated successfully"); // 토큰 전체 제거
            return true;
        } catch (ExpiredJwtException e) {
            logger.error("Token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public List<String> extractRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 서명 키 설정
                .build()                  // JwtParser 객체 생성
                .parseClaimsJws(token)    // 토큰 파싱
                .getBody();               // Claims 추출

        return claims.get("roles", List.class);
    }
}