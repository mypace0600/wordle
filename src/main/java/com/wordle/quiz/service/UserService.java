package com.wordle.quiz.service;

import com.wordle.quiz.dto.GoogleAuthRequest;
import com.wordle.quiz.dto.RankingResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true) // 조회 전용 트랜잭션으로 성능 최적화
    public UserResponse getUserInfo(String email) {
        log.info("Fetching user info for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new IllegalArgumentException("User with email " + email + " not found");
                });

        log.info("User found: email={}, id={}, score={}", user.getEmail(), user.getId(), user.getScore());

        // UserResponse 생성자 활용
        return new UserResponse(user.getId(), user.getEmail(), user.getScore());
    }

    public List<RankingResponse> getRankingList(String userId) {
        return null;
    }
}
