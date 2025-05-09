package com.wordle.quiz.dto;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizStartResponse {
    private Long quizId;
    private int wordLength;
    private Long nextQuizId;
}
