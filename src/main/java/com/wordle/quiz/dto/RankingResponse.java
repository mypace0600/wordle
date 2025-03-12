package com.wordle.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {
        private Long userId;
        private String email;
        private int score;
        private int rank;

}

