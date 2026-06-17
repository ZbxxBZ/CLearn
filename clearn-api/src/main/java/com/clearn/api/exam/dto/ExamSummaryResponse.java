package com.clearn.api.exam.dto;

import java.time.OffsetDateTime;

public record ExamSummaryResponse(
        Long id,
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
