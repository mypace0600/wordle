package com.wordle.quiz.dto;

import com.wordle.quiz.enums.LetterStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public  class LetterResult {
    private char letter;  // 사용자가 입력한 글자
    private LetterStatus status; // "correct" (🟩), "misplaced" (🟨), "incorrect" (⬜)
}
