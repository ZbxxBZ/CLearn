package com.clearn.api.exam.dto;

public record ExamProblemResponse(
        Long problemId,
        String title,
        String difficulty,
        Integer score,
        Integer sortOrder
) {
}
