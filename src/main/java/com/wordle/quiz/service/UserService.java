package com.wordle.quiz.service;

import com.wordle.quiz.dto.RankResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisTemplate<String, String> redisTemplate;

    private final int DEFAULT_HEARTS = 3;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserResponse(user.getId(), user.getEmail(), user.getType(), user.getScore());
    }

    public List<RankResponse> getRankingList(String userId) {
        return null;
    }

    public void initHearts(String email) {
        String key = "user:" + email + ":hearts";
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().set(key, String.valueOf(DEFAULT_HEARTS));
        }
    }
}
