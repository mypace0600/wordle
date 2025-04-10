package com.wordle.quiz.exception;

import com.wordle.quiz.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(null, ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(null, ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(null, message, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        ex.printStackTrace(); // 디버깅용 (운영 시에는 로거로)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(null, "예기치 못한 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @ExceptionHandler(NoAvailableQuizException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoAvailableQuizException(NoAvailableQuizException ex) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(null, ex.getMessage(), HttpStatus.NO_CONTENT.value()));
    }
}
