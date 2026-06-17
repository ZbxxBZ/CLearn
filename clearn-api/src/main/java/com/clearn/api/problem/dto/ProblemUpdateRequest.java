package com.clearn.api.problem.dto;

import java.util.List;

public record ProblemUpdateRequest(
        String title,
        String description,
        String inputDescription,
        String outputDescription,
        String difficulty,
        String tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer score,
        Boolean enabled,
        List<TestCaseCreateRequest> judgeCases,
        List<TestCaseCreateRequest> samples
) {
}
