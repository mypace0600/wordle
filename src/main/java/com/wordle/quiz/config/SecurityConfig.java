package com.wordle.quiz.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins:http://52.78.83.202:5173, http://13.125.255.85:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CustomOAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new CustomOAuth2AuthenticationSuccessHandler(jwtUtil);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(auth -> auth.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(allowedOrigins);
                    // 여기서 Origin 값에 맞게 CORS 설정
                    String origin = request.getHeader("Origin");
                    if (origin != null && (origin.contains("52.78.83.202") || origin.contains("13.125.255.85"))) {
                        config.setAllowedOrigins(List.of(origin));
                    } else {
                        config.setAllowedOrigins(List.of("http://localhost:5173")); // 기본적으로 로컬 서버도 허용
                    }
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/oauth2/**", "/callback", "/api/auth/**").permitAll()
                        .requestMatchers("/api/user", "/api/user/ranking").authenticated() // 인증 필요
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/quiz/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler()))
                .logout(logout->logout.disable())
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setContentType("application/json");
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.getWriter().write("{\"error\": \"Unauthorized\"}");
                        }));

        return http.build();
    }
}