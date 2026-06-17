package com.clearn.api.problem.dto;

public record TestCaseResponse(
        Long id,
        String inputData,
        String expectedOutput,
        boolean sample,
        Integer sortOrder
) {
}
