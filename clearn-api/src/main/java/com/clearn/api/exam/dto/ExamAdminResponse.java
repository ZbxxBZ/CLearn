package com.clearn.api.exam.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExamAdminResponse(
        Long id,
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Boolean enabled,
        List<ExamProblemResponse> problems
) {
}
