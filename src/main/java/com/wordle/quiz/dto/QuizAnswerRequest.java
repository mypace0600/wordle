package com.wordle.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {
    private Long quizId;
    private String answer; // 사용자가 제출한 단어
}
