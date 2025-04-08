package com.wordle.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedQuizResponse {
    private List<QuizResponse> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;

    // 생성자, getter/setter
    public PagedQuizResponse(List<QuizResponse> content, int totalPages, long totalElements, int currentPage) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
    }

    // Getters and Setters (롬복 @Data 써도 됨)
}
