package com.wordle.quiz.repository;

import com.wordle.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {


    boolean existsByAnswer(String answer);

    Page<Quiz> findByAnswerContainingIgnoreCase(String keyword, Pageable pageable);
}
