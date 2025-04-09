package com.wordle.quiz.dto;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizStartResponse {
    private Long quizId;
    private int wordLength;

    @Builder.Default
    private int maxAttempts = 4;

    private Long nextQuizId;
}
