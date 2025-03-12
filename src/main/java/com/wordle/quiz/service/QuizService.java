package com.wordle.quiz.service;

import com.wordle.quiz.dto.*;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

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

    public QuizResponse createQuiz(QuizRequest request) {
        log.info("Creating quiz with answer: {}", request.getAnswer());
        Quiz quiz = new Quiz();
        quiz.setAnswer(request.getAnswer());
        quiz.setHint(request.getHint());
        Quiz savedQuiz = quizRepository.save(quiz);

        return QuizResponse.builder()
                .id(savedQuiz.getId())
                .answer(savedQuiz.getAnswer())
                .hint(savedQuiz.getHint())
                .createdAt(savedQuiz.getCreatedAt())
                .updatedAt(savedQuiz.getUpdatedAt())
                .build();
    }

    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        log.info("Updating quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quiz.setAnswer(request.getAnswer());
        quiz.setHint(request.getHint());
        Quiz updatedQuiz = quizRepository.save(quiz);

        return QuizResponse.builder()
                .id(updatedQuiz.getId())
                .answer(updatedQuiz.getAnswer())
                .hint(updatedQuiz.getHint())
                .createdAt(updatedQuiz.getCreatedAt())
                .updatedAt(updatedQuiz.getUpdatedAt())
                .build();
    }

    public void deleteQuiz(Long id) {
        log.info("Deleting quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quizRepository.delete(quiz);
    }
}
