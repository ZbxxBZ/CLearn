package com.clearn.api.submission.dto;

import java.time.OffsetDateTime;

public record SubmissionResponse(
        Long id,
        Long problemId,
        Long examId,
        String language,
        String sourceCode,
        String status,
        Integer score,
        Long timeUsedMs,
        Long memoryUsedKb,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime judgedAt
) {
}
