package com.wordle.quiz.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    // 미인증 상태 토큰 생성자
    public JwtAuthenticationToken(String principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false);
    }

    // 인증된 상태 토큰 생성자
    public JwtAuthenticationToken(String principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // JWT는 비밀번호 없이 토큰으로 인증
    }

    @Override
    public Object getPrincipal() {
        return principal; // 사용자 이메일 또는 ID
    }
}