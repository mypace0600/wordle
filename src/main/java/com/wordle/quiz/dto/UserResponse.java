package com.wordle.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordle.quiz.entity.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String email;
    @JsonProperty("isAdmin") // 명시적으로 isAdmin 보장 (필요 시 추가)
    private boolean isAdmin;
    private int score;
}

