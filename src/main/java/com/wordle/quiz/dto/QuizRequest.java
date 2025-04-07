package com.wordle.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
public class QuizRequest {
    @NotBlank(message = "Answer cannot be blank")
    private String answer;

    private String hint; // 선택 필드이므로 검증 없음
}
