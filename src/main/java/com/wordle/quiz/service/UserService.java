package com.wordle.quiz.service;

import com.wordle.quiz.dto.HeartStatus;
import com.wordle.quiz.dto.RankResponse;
import com.wordle.quiz.dto.StatResponse;
import com.wordle.quiz.dto.UserResponse;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.entity.UserQuiz;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserQuizRepository;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisTemplate<String, String> redisTemplate;

    private final int DEFAULT_HEARTS = 3;

    private final UserRepository userRepository;
    private final UserQuizRepository userQuizRepository;
    private final QuizRepository quizRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserResponse(user.getId(), user.getEmail(), user.getType(), user.getScore());
    }

    public List<RankResponse> getRankingList(String userId) {
        return null;
    }

    public void initHearts(String email) {
        String heartKey = "user:" + email + ":hearts";
        String lastUsedKey = "user:" + email + ":hearts:last-used";

        if (Boolean.FALSE.equals(redisTemplate.hasKey(heartKey))) {
            redisTemplate.opsForValue().set(heartKey, String.valueOf(DEFAULT_HEARTS));
            redisTemplate.opsForValue().set(lastUsedKey, String.valueOf(Instant.now().getEpochSecond()));
        }
    }


    public HeartStatus getHeartStatus(String email) {
        String heartKey = "user:" + email + ":hearts";
        String lastUsedKey = "user:" + email + ":hearts:last-used";

        int hearts = Integer.parseInt(
                Optional.ofNullable(redisTemplate.opsForValue().get(heartKey)).orElse("3")
        );

        long lastUsedAt = Optional.ofNullable(redisTemplate.opsForValue().get(lastUsedKey))
                .map(Long::parseLong)
                .orElse(System.currentTimeMillis() / 1000); // fallback

        return new HeartStatus(hearts, lastUsedAt);
    }

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
