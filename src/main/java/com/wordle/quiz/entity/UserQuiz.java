package com.wordle.quiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_quiz", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "quiz_id"})
})
public class UserQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean solved = false;

    @Column
    private LocalDateTime lastTriedAt;

    // ✅ 이 생성자 추가
    public UserQuiz(User user, Quiz quiz, int attempts, boolean solved, LocalDateTime lastTriedAt) {
        this.user = user;
        this.quiz = quiz;
        this.attempts = attempts;
        this.solved = solved;
        this.lastTriedAt = lastTriedAt;
    }
}
