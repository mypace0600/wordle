package com.wordle.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedUserResponse {
    private List<UserResponse> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;

    // 생성자, getter/setter
    public PagedUserResponse(List<UserResponse> content, int totalPages, long totalElements, int currentPage) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
    }

    // Getters and Setters (롬복 @Data 써도 됨)
}
