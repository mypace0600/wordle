package com.wordle.quiz.dto;


import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatResponse {

    private int solvedCount;
    private double percentile;    // ex: 12.5 (Top 12.5%)
    private int totalQuizCount;     // 전체 퀴즈 수 (👈 추가됨)
}

