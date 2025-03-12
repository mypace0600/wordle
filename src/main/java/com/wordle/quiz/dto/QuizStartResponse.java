package com.wordle.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizStartResponse {
    private Long quizId;
    private int wordLength;  // Wordle 단어 길이 (예: 5글자)
    private int maxAttempts = 4; // 최대 시도 횟수
}
