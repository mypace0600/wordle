package com.wordle.quiz.service;

import com.wordle.quiz.dto.StatResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;
    private final UserQuizRepository userQuizRepository;
    private final QuizRepository quizRepository;



    public StatResponse getUserStatistics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        List<UserQuiz> attempts = userQuizRepository.findAllByUser(user);
        int solvedCount = (int) attempts.stream().filter(UserQuiz::isSolved).count();

        // 전체 유저 수
        long totalUsers = userRepository.count();

        // 나보다 점수 낮은 유저 수
        long lowerScoreUsers = userRepository.countByScoreLessThan(user.getScore());

        // 퍼센타일 계산 (자기보다 점수 낮은 유저 / 전체 유저) * 100
        double percentile = totalUsers == 0 ? 0.0 : ((double) lowerScoreUsers / totalUsers) * 100.0;

        // 전체 퀴즈 수
        int totalQuizCount = (int) quizRepository.count();

        return new StatResponse(solvedCount, percentile, totalQuizCount);

    }
}
