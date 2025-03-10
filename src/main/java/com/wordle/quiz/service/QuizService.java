package com.wordle.quiz.service;

import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.dto.QuizStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuizService {
    public QuizStartResponse startQuiz(String userId) {
        return null;
    }

    public QuizResultResponse submitAnswer(String userId, QuizAnswerRequest request) {
        return null;
    }

    public void viewAdvertisement(String userId) {
    }

    public QuizStatusResponse getQuizStatus(String userId, String quizId) {
        return null;
    }
}
