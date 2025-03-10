package com.wordle.quiz.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    // 생성자
    public JwtAuthenticationToken(String principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false); // 처음에는 인증되지 않은 상태로 설정
    }

    // 인증된 후 사용자 정보 (권한 추가)
    public JwtAuthenticationToken(String principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // JWT 토큰은 인증에 필요한 비밀번호 없이 진행되므로 null 반환
    }

    @Override
    public Object getPrincipal() {
        return principal; // JWT에서 추출된 사용자 ID 또는 사용자 이름
    }
}
