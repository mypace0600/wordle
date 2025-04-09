package com.wordle.quiz.dto;

import com.wordle.quiz.enums.LetterStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {
    private boolean correct;  // 정답 여부
    private int score;        // 획득한 점수
    private int remainingAttempts; // 남은 시도 횟수
    private List<LetterResult> feedback; // 각 글자의 Wordle 스타일 피드백


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LetterResult {
        private char letter;  // 사용자가 입력한 글자
        private LetterStatus status; // "correct" (🟩), "misplaced" (🟨), "incorrect" (⬜)
    }
}

