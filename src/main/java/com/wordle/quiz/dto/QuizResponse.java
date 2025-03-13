package com.wordle.quiz.dto;

import lombok.*;


@Getter
@Builder
public class QuizResponse {

    private Long id;
    private String answer;
    private String hint;

    public QuizResponse(Long id, String answer, String hint) {
        this.id = id;
        this.answer = answer;
        this.hint = hint;
    }
}
