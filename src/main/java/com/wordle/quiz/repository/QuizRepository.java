package com.wordle.quiz.repository;

import com.wordle.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    boolean existsByAnswer(String answer);

    Page<Quiz> findByAnswerContainingIgnoreCase(String keyword, Pageable pageable);


    @Query("SELECT q FROM Quiz q WHERE q.id NOT IN :solvedIds ORDER BY function('RAND')")
    List<Quiz> findUnsolvedQuizzesRandomly(List<Long> solvedIds, PageRequest pageRequest);
}
