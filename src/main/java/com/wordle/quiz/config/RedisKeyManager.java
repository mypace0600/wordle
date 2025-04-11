package com.wordle.quiz.config;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyManager {

    public String getHeartKey(String email) {
        return "user:" + email + ":hearts";
    }

    public String getLastHeartUsedKey(String email) {
        return "user:" + email + ":hearts:last-used";
    }

    public String getAttemptKey(String email, Long quizId) { return "user:" + email + ":quiz:" + quizId + ":attempts"; }

    public String getUnsolvedKey(String email) { return "unsolved_quizzes:" + email; }
}

