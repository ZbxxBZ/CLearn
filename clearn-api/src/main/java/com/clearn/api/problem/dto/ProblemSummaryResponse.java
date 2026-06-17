package com.clearn.api.problem.dto;

public record ProblemSummaryResponse(
        Long id,
        String title,
        String difficulty,
        String tags,
        Integer score
) {
}
