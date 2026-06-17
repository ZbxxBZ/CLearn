package com.clearn.api.exam.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExamDetailResponse(
        Long id,
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<ExamProblemResponse> problems
) {
}
