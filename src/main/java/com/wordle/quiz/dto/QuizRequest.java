package com.wordle.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class QuizRequest {
    @NotBlank(message = "Answer cannot be blank")
    private String answer;

    private String hint; // optional
}
