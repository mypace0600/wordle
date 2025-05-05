package com.wordle.quiz.dto;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PagedQuizResponse {
    private List<QuizResponse> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
