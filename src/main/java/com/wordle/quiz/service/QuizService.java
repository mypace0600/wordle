package com.wordle.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordle.quiz.dto.LetterResult;
import com.wordle.quiz.dto.QuizAnswerRequest;
import com.wordle.quiz.dto.QuizResultResponse;
import com.wordle.quiz.dto.QuizStartResponse;
import com.wordle.quiz.entity.Quiz;
import com.wordle.quiz.entity.User;
import com.wordle.quiz.enums.LetterStatus;
import com.wordle.quiz.repository.QuizRepository;
import com.wordle.quiz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_HEARTS = 3;
    private static final int SCORE_PER_CORRECT = 10;

    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);

        List<Quiz> cached = getCachedUnsolvedQuizzes(email);
        if (cached.isEmpty()) {
            List<Quiz> all = quizRepository.findAll();
            Set<Long> solvedIds = user.getQuizAttempts().stream()
                    .map(attempt -> attempt.getQuiz().getId())
                    .collect(Collectors.toSet());

            List<Quiz> unsolved = all.stream()
                    .filter(q -> !solvedIds.contains(q.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(unsolved);
            cacheUnsolvedQuizzes(email, unsolved);
            cached = new ArrayList<>(unsolved);
        }

        if (cached.isEmpty()) throw new IllegalStateException("풀 수 있는 퀴즈가 없습니다.");

        Quiz quiz = cached.remove(0);
        Quiz nextQuiz = !cached.isEmpty() ? cached.get(0) : null;

        cacheUnsolvedQuizzes(email, cached);

        return QuizStartResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(nextQuiz != null ? nextQuiz.getId() : null)
                .build();
    }

    public QuizStartResponse getQuizDetails(String email, Long quizId) {
        Quiz quiz = getQuizById(quizId);

        return QuizStartResponse.builder()
                .quizId(quiz.getId())
                .wordLength(quiz.getAnswer().length())
                .nextQuizId(null) // 그냥 디테일이니까 nextQuiz는 null 처리
                .build();
    }


    public QuizResultResponse submitAnswer(String email, QuizAnswerRequest request) {
        User user = getUser(email);
        Quiz quiz = getQuizById(request.getQuizId());

        int attempts = getAttempts(user.getId(), quiz.getId());
        int hearts = getHearts(user.getId());

        if (attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("최대 시도 횟수를 초과했습니다.");
        }
        if (hearts <= 0) {
            throw new IllegalStateException("하트가 부족합니다.");
        }

        incrementAttempts(user.getId(), quiz.getId());

        String answer = quiz.getAnswer().toLowerCase();
        String submitted = request.getAnswer().toLowerCase();
        List<Character> submittedChars = submitted.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());

        List<LetterResult> feedback = generateFeedback(answer, submittedChars);
        boolean isCorrect = feedback.stream().allMatch(r -> r.getStatus() == LetterStatus.CORRECT);

        if (isCorrect) {
            resetAttempts(user.getEmail(), quiz.getId());
            deleteUnsolvedCache(email);
            user.setScore(user.getScore() + SCORE_PER_CORRECT);
            userRepository.save(user);
        } else {
            decrementHearts(user.getId());
        }

        return new QuizResultResponse(
                isCorrect,
                user.getScore(),
                MAX_ATTEMPTS - getAttempts(user.getId(), quiz.getId()),
                feedback
        );
    }

    public void resetAttempts(String email, Long quizId) {
        User user = getUser(email);
        redisTemplate.delete(getAttemptKey(user.getId(), quizId));
    }

    // ====== Feedback 생성 ======

    private List<LetterResult> generateFeedback(String answer, List<Character> submitted) {
        List<LetterResult> result = new ArrayList<>();
        boolean[] used = new boolean[answer.length()];

        // First pass: 정확한 위치
        for (int i = 0; i < submitted.size(); i++) {
            char ch = submitted.get(i);
            if (i < answer.length() && ch == answer.charAt(i)) {
                used[i] = true;
                result.add(new LetterResult(ch, LetterStatus.CORRECT));
            } else {
                result.add(null); // 임시 placeholder
            }
        }

        // Second pass: 다른 위치 존재 여부
        for (int i = 0; i < submitted.size(); i++) {
            if (result.get(i) != null) continue;

            char ch = submitted.get(i);
            boolean found = false;
            for (int j = 0; j < answer.length(); j++) {
                if (!used[j] && answer.charAt(j) == ch) {
                    used[j] = true;
                    found = true;
                    break;
                }
            }
            result.set(i, new LetterResult(ch, found ? LetterStatus.MISPLACED : LetterStatus.INCORRECT));
        }

        return result;
    }

    // ====== Redis 유틸 ======

    private List<Quiz> getCachedUnsolvedQuizzes(String email) {
        String key = getUnsolvedKey(email);
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null) return new ArrayList<>();
        return jsonList.stream().map(json -> {
            try {
                return objectMapper.readValue(json, Quiz.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void cacheUnsolvedQuizzes(String email, List<Quiz> quizzes) {
        String key = getUnsolvedKey(email);
        redisTemplate.delete(key);
        List<String> jsonList = quizzes.stream().map(q -> {
            try {
                return objectMapper.writeValueAsString(q);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (!jsonList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, jsonList);
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }

    private void deleteUnsolvedCache(String email) {
        redisTemplate.delete(getUnsolvedKey(email));
    }

    // ====== 하트 & 시도 관련 ======

    private int getHearts(Long userId) {
        String key = getHeartKey(userId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : MAX_HEARTS;
    }

    private void decrementHearts(Long userId) {
        redisTemplate.opsForValue().decrement(getHeartKey(userId));
    }

    private int getAttempts(Long userId, Long quizId) {
        String key = getAttemptKey(userId, quizId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    private void incrementAttempts(Long userId, Long quizId) {
        String key = getAttemptKey(userId, quizId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(1));
    }

    // ====== 기타 ======

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private Quiz getQuizById(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("퀴즈를 찾을 수 없습니다."));
    }

    private String getHeartKey(Long userId) {
        return "user:" + userId + ":hearts";
    }

    private String getAttemptKey(Long userId, Long quizId) {
        return "user:" + userId + ":quiz:" + quizId + ":attempts";
    }

    private String getUnsolvedKey(String email) {
        return "unsolved_quizzes:" + email;
    }
}
