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
    private int wordLength;
    private int maxAttempts = 4;
    private Long prevQuizId; // 추가
    private Long nextQuizId; // 추가
}
