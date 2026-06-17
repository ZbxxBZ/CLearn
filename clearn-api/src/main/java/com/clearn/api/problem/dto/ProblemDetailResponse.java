package com.clearn.api.problem.dto;

import java.util.List;

public record ProblemDetailResponse(
        Long id,
        String title,
        String description,
        String inputDescription,
        String outputDescription,
        String difficulty,
        String tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer score,
        List<TestCaseResponse> samples
) {
}
