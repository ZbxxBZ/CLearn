package com.clearn.api.exam.dto;

public record ExamProblemCreateRequest(
        Long problemId,
        Integer score,
        Integer sortOrder
) {
}
