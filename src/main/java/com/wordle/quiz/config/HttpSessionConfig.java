package com.wordle.quiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class HttpSessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");                 // 기본은 SESSION
        serializer.setDomainName("hyeonsu-side.com");        // 최상위 도메인으로 설정
        serializer.setUseSecureCookie(true);                 // HTTPS 전용
        serializer.setSameSite("None");                      // cross-site 허용
        return serializer;
    }
}