package com.wordle.quiz.repository;

import com.wordle.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    boolean existsByAnswer(String answer);

    Page<Quiz> findByAnswerContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(
            value = "SELECT * FROM quiz WHERE id NOT IN (:solvedIds) ORDER BY RAND() LIMIT :limit",
            nativeQuery = true
    )
    List<Quiz> findUnsolvedQuizzesRandomlyNative(
            @Param("solvedIds") List<Long> solvedIds,
            @Param("limit") int limit
    );
}
