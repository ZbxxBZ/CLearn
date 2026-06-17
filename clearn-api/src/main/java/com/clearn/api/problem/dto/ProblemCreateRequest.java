package com.clearn.api.problem.dto;

public record ProblemCreateRequest(
        String title,
        String description,
        String inputDescription,
        String outputDescription,
        String difficulty,
        String tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer score,
        Boolean enabled
) {
}
