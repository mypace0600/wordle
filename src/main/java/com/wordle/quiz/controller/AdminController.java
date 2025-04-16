package com.wordle.quiz.controller;

import com.wordle.quiz.dto.*;
import com.wordle.quiz.service.AdminQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminQuizService adminQuizService;

    @GetMapping("/user/list")
    public ResponseEntity<ApiResponse<PagedUserResponse>> userList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword){
        Pageable pageable = PageRequest.of(page,size);
        Page<UserResponse> userPage = adminQuizService.getUserList(pageable,keyword);
        PagedUserResponse response = new PagedUserResponse(
                userPage.getContent(),
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getNumber()
        );
        return ResponseEntity.ok(new ApiResponse<>(response,"유저 목록 조회 성공",200));
    }

    // 퀴즈 목록
    @GetMapping("/quiz/list")
    public ResponseEntity<ApiResponse<PagedQuizResponse>> quizList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResponse> quizPage = adminQuizService.getQuizList(pageable, keyword);

        PagedQuizResponse response = new PagedQuizResponse(
                quizPage.getContent(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements(),
                quizPage.getNumber()
        );

        return ResponseEntity.ok(new ApiResponse<>(response, "퀴즈 목록 조회 성공", 200));
    }

    // 퀴즈 생성
    @PostMapping("/quiz/create")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(@RequestBody @Validated QuizRequest request) {
        QuizResponse response = adminQuizService.createQuiz(request);
        return ResponseEntity.ok(new ApiResponse<>(response, "퀴즈 생성 성공", 201));
    }

    // 퀴즈 수정
    @PutMapping("/quiz/update/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable Long id,
            @RequestBody @Validated QuizRequest request
    ) {
        QuizResponse response = adminQuizService.updateQuiz(id, request);
        return ResponseEntity.ok(new ApiResponse<>(response, "퀴즈 수정 성공", 200));
    }

    // 퀴즈 삭제
    @DeleteMapping("/quiz/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Long id) {
        adminQuizService.deleteQuiz(id);
        return ResponseEntity.ok(new ApiResponse<>(null, "퀴즈 삭제 성공", 200));
    }
}
