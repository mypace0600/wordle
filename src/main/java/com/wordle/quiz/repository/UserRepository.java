package com.wordle.quiz.repository;

import com.wordle.quiz.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    long countByScoreLessThan(int score);

    Page<User> findByEmailContainingIgnoreCase(String keyword, Pageable pageable);
}
