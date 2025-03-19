package com.wordle.quiz.controller;

import com.wordle.quiz.dto.QuizRequest;
import com.wordle.quiz.dto.QuizResponse;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/quiz")
public class AdminController {

    private final QuizService quizService;

    @GetMapping("/list") // 경로는 필요에 따라 수정
    public ResponseEntity<List<QuizResponse>> quizList(
            @RequestParam(defaultValue = "0") int page, // 페이지 번호 (0부터 시작)
            @RequestParam(defaultValue = "10") int size // 페이지당 row 수, 기본값 10
    ) {
        Pageable pageable = PageRequest.of(page, size); // Pageable 객체 생성
        Page<QuizResponse> quizPage = quizService.getQuizList(pageable); // 서비스 호출
        List<QuizResponse> quizList = quizPage.getContent(); // 페이지 내용 추출
        return ResponseEntity.ok(quizList);
    }

    @PostMapping("/create")
    public ResponseEntity<QuizResponse> createQuiz(@RequestBody QuizRequest request) {
        QuizResponse response = quizService.createQuiz(request);
        return ResponseEntity.ok(response);
    }

    // 퀴즈 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<QuizResponse> updateQuiz(@PathVariable Long id, @RequestBody QuizRequest request) {
        QuizResponse response = quizService.updateQuiz(id, request);
        return ResponseEntity.ok(response);
    }

    // 퀴즈 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}
