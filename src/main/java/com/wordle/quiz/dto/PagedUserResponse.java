package com.wordle.quiz.dto;

import lombok.*;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PagedUserResponse {
    private List<UserResponse> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
