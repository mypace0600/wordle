package com.wordle.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuizAnswerRequest {
    @NotNull(message = "Quiz ID cannot be null")
    private Long quizId;

    @NotBlank(message = "Answer cannot be blank")
    private String answer;
}
