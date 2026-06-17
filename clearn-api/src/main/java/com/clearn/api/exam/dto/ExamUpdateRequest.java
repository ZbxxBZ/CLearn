package com.clearn.api.exam.dto;

import java.time.OffsetDateTime;

public record ExamUpdateRequest(
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Boolean enabled
) {
}
