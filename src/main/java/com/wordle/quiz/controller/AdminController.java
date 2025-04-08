package com.wordle.quiz.controller;

import com.wordle.quiz.dto.PagedQuizResponse;
import com.wordle.quiz.dto.QuizRequest;
import com.wordle.quiz.dto.QuizResponse;
import com.wordle.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/quiz")
public class AdminController {

    private final QuizService quizService;

    @GetMapping("/list")
    public ResponseEntity<PagedQuizResponse> quizList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        log.info("@@@@@@@@@@@@@ keyword :{}",keyword);
        Pageable pageable = PageRequest.of(page, size);
        Page<QuizResponse> quizPage = quizService.getQuizList(pageable,keyword);

        PagedQuizResponse response = new PagedQuizResponse(
                quizPage.getContent(),
                quizPage.getTotalPages(),
                quizPage.getTotalElements(),
                quizPage.getNumber()
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/create")
    public ResponseEntity<?> createQuiz(@RequestBody QuizRequest request) {
        try {
            QuizResponse response = quizService.createQuiz(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict
                    .body(new ErrorResponse("DUPLICATE_QUIZ", e.getMessage()));
        }
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
record ErrorResponse(String code, String message) {}