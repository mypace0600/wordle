package com.wordle.quiz.exception;

public class NoAvailableQuizException extends RuntimeException {
    public NoAvailableQuizException(String message) {
        super(message);
    }
}
