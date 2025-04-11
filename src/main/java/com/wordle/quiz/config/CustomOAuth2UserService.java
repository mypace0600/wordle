package com.wordle.quiz.config;

import com.wordle.quiz.entity.User;
import com.wordle.quiz.enums.UserType;
import com.wordle.quiz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RedisUserStateService redisUserStateService;
    private final UserRepository userRepository;


    @Value("${admin.email:mypace0600@gmail.com}")
    private String adminEmail;

    public CustomOAuth2UserService(UserRepository userRepository, RedisUserStateService redisUserStateService) {
        this.userRepository = userRepository;
        this.redisUserStateService = redisUserStateService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        AtomicBoolean isNewUser = new AtomicBoolean(false);
        // 이메일 형식 검증
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // 사용자 조회 또는 생성
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setScore(0);
                    newUser.setType(UserType.USER); // 기본값 USER
                    newUser.setAdmin(false); // 기본값
                    isNewUser.set(true);
                    return userRepository.save(newUser);
                });

        // 권한 부여
        List<GrantedAuthority> authorities;
        if (email.equals(adminEmail)) {
            user.setType(UserType.ADMIN);
            user.setAdmin(true);
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 사용자 정보 업데이트 (필요 시)
        userRepository.save(user);

        // ✅ 최초 로그인 시 하트 초기화
        if (isNewUser.get()) {
            redisUserStateService.initHearts(email);
        }

        return new CustomOAuth2User(oAuth2User.getAttributes(), authorities);
    }
}