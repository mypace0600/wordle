package com.wordle.quiz.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordle.quiz.dto.HeartStatus;
import com.wordle.quiz.entity.Quiz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisUserStateService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisKeyManager keyManager;

    private static final int DEFAULT_HEARTS = 3;

    // 하트 상태 초기화: 최초 가입 시, 또는 광고 시청 시, redis에 정보가 없으면 hearts 갯수를 3개로 인식
    public void initHearts(String email) {
        // Redis에서 하트 관련 키들이 존재하면 삭제
        for (int i = 0; i < DEFAULT_HEARTS; i++) {
            String heartUsedKey = keyManager.getHeartMinusKey(email, i);
            redisTemplate.delete(heartUsedKey);
        }
    }

    // 하트 개수 조회
    public int getHearts(String email) {
        int remainingHearts = DEFAULT_HEARTS;
        for (int i = 0; i < DEFAULT_HEARTS; i++) {
            String heartUsedKey = keyManager.getHeartMinusKey(email, i);
            if (redisTemplate.hasKey(heartUsedKey)) {
                remainingHearts--;
            }
        }
        return remainingHearts;
    }

    private List<Integer> getUsedHeartIndexes(String email) {
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < DEFAULT_HEARTS; i++) {
            String key = keyManager.getHeartMinusKey(email, i);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                used.add(i);
            }
        }
        return used;
    }

    // 하트 차감 및 차감 시점 기록
    public void decrementHearts(String email) {
        List<Integer> usedIndexes = getUsedHeartIndexes(email);

        if (usedIndexes.size() >= DEFAULT_HEARTS) {
            log.warn("All hearts are already used.");
            return;
        }

        // 사용되지 않은 인덱스 중 가장 작은 숫자 찾기
        int nextIndex = IntStream.range(0, DEFAULT_HEARTS)
                .filter(i -> !usedIndexes.contains(i))
                .findFirst()
                .orElseThrow();

        String key = keyManager.getHeartMinusKey(email, nextIndex);
        redisTemplate.opsForValue().set(key, String.valueOf(Instant.now().getEpochSecond()));

        log.info(">>> [After decrementHearts] email: {}, remain hearts: {}",email, getHearts(email));

        // TTL 설정 (1시간 후 자동 삭제)
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    // 하트 회복 처리 (1시간 경과 후 하트 회복)
    public void recoverHearts(String email) {
        for (int i = 0; i < DEFAULT_HEARTS; i++) {
            String heartUsedKey = keyManager.getHeartMinusKey(email, i);
            if (redisTemplate.hasKey(heartUsedKey)) {
                long lastUsedAt = Long.parseLong(redisTemplate.opsForValue().get(heartUsedKey));
                if (Instant.now().getEpochSecond() - lastUsedAt >= 3600) { // 1시간 경과
                    redisTemplate.delete(heartUsedKey);
                }
            }
        }
    }

    // 시도 횟수 조회
    public int getAttempts(String email, Long quizId) {
        String key = keyManager.getAttemptKey(email, quizId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    // 시도 횟수 증가
    public void incrementAttempts(String email, Long quizId) {
        String key = keyManager.getAttemptKey(email, quizId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    // 시도 횟수 초기화
    public void resetAttempts(String email, Long quizId) {
        redisTemplate.delete(keyManager.getAttemptKey(email, quizId));
    }

    // 하트 상태 조회
    public HeartStatus getHeartStatus(String email) {
        recoverHearts(email); // 하트 회복을 사전에 체크함
        int hearts = getHearts(email);
        return new HeartStatus(hearts, System.currentTimeMillis() / 1000);
    }

    // 안푼 퀴즈 캐시 조회
    public List<Quiz> getCachedUnsolvedQuizzes(String email) {
        String key = keyManager.getUnsolvedKey(email);
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null) return new ArrayList<>();
        return jsonList.stream().map(json -> {
            try {
                return new ObjectMapper().readValue(json, Quiz.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // 안푼 퀴즈 캐시 저장
    public void cacheUnsolvedQuizzes(String email, List<Quiz> quizzes) {
        String key = keyManager.getUnsolvedKey(email);
        redisTemplate.delete(key);
        List<String> jsonList = quizzes.stream().map(q -> {
            try {
                return new ObjectMapper().writeValueAsString(q);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (!jsonList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, jsonList);
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }

    // 안푼 퀴즈 캐시 삭제
    public void deleteUnsolvedCache(String email) {
        redisTemplate.delete(keyManager.getUnsolvedKey(email));
    }
}
