package com.wordle.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailResponse {
    private Long quizId;
    private int wordLength;
    private Long nextQuizId;
    private String answer;
    private String hint;
}
