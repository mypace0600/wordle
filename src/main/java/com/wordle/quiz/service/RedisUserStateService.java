package com.wordle.quiz.service;

import com.wordle.quiz.config.RedisKeyManager;
import com.wordle.quiz.dto.HeartStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisUserStateService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager keyManager;

    private static final int DEFAULT_HEARTS = 3;

    public void initHearts(String email) {
        String heartKey = keyManager.getHeartKey(email);
        String lastUsedKey = keyManager.getLastHeartUsedKey(email);

        if (Boolean.FALSE.equals(redisTemplate.hasKey(heartKey))) {
            redisTemplate.opsForValue().set(heartKey, String.valueOf(DEFAULT_HEARTS));
            redisTemplate.opsForValue().set(lastUsedKey, String.valueOf(Instant.now().getEpochSecond()));
        }
    }

    public int getHearts(String email) {
        String key = keyManager.getHeartKey(email);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : DEFAULT_HEARTS;
    }

    public void decrementHearts(String email) {
        redisTemplate.opsForValue().decrement(keyManager.getHeartKey(email));
    }

    public int getAttempts(String email, Long quizId) {
        String key = keyManager.getAttemptKey(email, quizId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    public void incrementAttempts(String email, Long quizId) {
        String key = keyManager.getAttemptKey(email, quizId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    public void resetAttempts(String email, Long quizId) {
        redisTemplate.delete(keyManager.getAttemptKey(email, quizId));
    }

    public HeartStatus getHeartStatus(String email) {
        int hearts = getHearts(email);
        long lastUsedAt = Optional.ofNullable(redisTemplate.opsForValue().get(keyManager.getLastHeartUsedKey(email)))
                .map(Long::parseLong)
                .orElse(System.currentTimeMillis() / 1000); // fallback
        return new HeartStatus(hearts, lastUsedAt);
    }
}
