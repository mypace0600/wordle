package com.wordle.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatusResponse {
    private Long quizId;
    private int remainingAttempts;
    private List<List<QuizResultResponse.LetterResult>> previousAttempts; // 이전 시도 기록
}

