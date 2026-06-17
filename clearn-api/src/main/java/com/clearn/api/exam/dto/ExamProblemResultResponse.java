package com.clearn.api.exam.dto;

public record ExamProblemResultResponse(
        Long problemId,
        String title,
        Integer score,
        Integer maxScore,
        Long bestSubmissionId,
        String bestStatus
) {
}
