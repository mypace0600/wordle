package com.wordle.quiz.dto;

import com.wordle.quiz.enums.UserType;
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
    private UserType userType;
    private int score;
}


