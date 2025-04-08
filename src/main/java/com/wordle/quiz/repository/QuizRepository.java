package com.wordle.quiz.repository;

import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @Query("SELECT q FROM Quiz q WHERE q NOT IN " +
            "(SELECT uq.quiz FROM UserQuiz uq WHERE uq.user = :user AND uq.isSolved = true)")
    List<Quiz> findUnsolvedQuizzesByUser(User user);

    boolean existsByAnswer(String answer);

    Page<Quiz> findByAnswerContainingIgnoreCase(String keyword, Pageable pageable);
}
