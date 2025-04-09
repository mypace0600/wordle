package com.wordle.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequest {
    @NotBlank(message = "Answer cannot be blank")
    private String answer;

    private String hint; // optional
}
