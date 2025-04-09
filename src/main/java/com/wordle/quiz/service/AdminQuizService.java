package com.wordle.quiz.service;

import com.wordle.quiz.dto.QuizRequest;
import com.wordle.quiz.dto.QuizResponse;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.repository.QuizRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuizService {

    private final QuizRepository quizRepository;

    // 퀴즈 생성 (어드민)
    public QuizResponse createQuiz(@Valid QuizRequest request) {
        log.info("Creating quiz with answer: {}", request.getAnswer());
        if (quizRepository.existsByAnswer(request.getAnswer())) {
            log.warn("Duplicate quiz answer detected: {}", request.getAnswer());
            throw new IllegalArgumentException("이미 등록된 퀴즈입니다.");
        }

        Quiz quiz = new Quiz();
        quiz.setAnswer(request.getAnswer());
        quiz.setHint(request.getHint());
        Quiz savedQuiz = quizRepository.save(quiz);

        return QuizResponse.builder()
                .id(savedQuiz.getId())
                .answer(savedQuiz.getAnswer())
                .hint(savedQuiz.getHint())
                .build();
    }

    // 퀴즈 수정 (어드민)
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
                .build();
    }

    // 퀴즈 삭제 (어드민)
    public void deleteQuiz(Long id) {
        log.info("Deleting quiz with id: {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found with id: " + id));
        quizRepository.delete(quiz);
    }

    // 퀴즈 목록 조회 (어드민)
    public Page<QuizResponse> getQuizList(Pageable pageable, String keyword) {
        Page<Quiz> quizPage;
        if (keyword != null && !keyword.isBlank()) {
            quizPage = quizRepository.findByAnswerContainingIgnoreCase(keyword, pageable);
        } else {
            quizPage = quizRepository.findAll(pageable);
        }

        return quizPage.map(quiz -> new QuizResponse(
                quiz.getId(),
                quiz.getAnswer(),
                quiz.getHint()
        ));
    }
}
