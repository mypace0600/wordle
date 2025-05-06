package com.wordle.quiz.repository;

import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserQuizRepository extends JpaRepository<UserQuiz,Long> {
    Optional<UserQuiz> findByUserIdAndQuizId(Long userId, Long quizId);

    @Query("SELECT uq.quiz.id FROM UserQuiz uq WHERE uq.user.id = :userId AND uq.solved = true")
    List<Long> findSolvedQuizIdsByUser(@Param("userId") Long userId);

    List<UserQuiz> findAllByUser(User user);

    boolean existsByUserIdAndQuizId(Long userId, Long quizId);
}
