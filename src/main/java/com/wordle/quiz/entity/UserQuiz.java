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
    private boolean isSolved = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserQuiz(User user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
    }

}
