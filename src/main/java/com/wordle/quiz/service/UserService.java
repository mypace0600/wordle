package com.wordle.quiz.service;

import com.wordle.quiz.dto.GoogleAuthRequest;
import com.wordle.quiz.dto.RankingResponse;
import com.wordle.quiz.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    public String authenticateWithGoogle(GoogleAuthRequest request) {
        return null;
    }

    public UserResponse getUserInfo(String userId) {
        return null;
    }

    public List<RankingResponse> getRankingList(String userId) {
        return null;
    }
}
