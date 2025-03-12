package com.wordle.quiz.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuizResponse {

    private Long id;
    private String answer;
    private String hint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
