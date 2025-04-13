package com.wordle.quiz.config;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyManager {

    // 하트 차감 시각 키 생성 (하트 인덱스별 차감 시각)
    public String getHeartMinusKey(String email, int index) {
        return "user:" + email + ":heart:" + index + ":minusDt";
    }

    public String getAttemptKey(String email, Long quizId) { return "user:" + email + ":quiz:" + quizId + ":attempts"; }

    public String getUnsolvedKey(String email) { return "unsolved_quizzes:" + email; }
}

