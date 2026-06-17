package com.clearn.api.problem.dto;

public record TestCaseCreateRequest(
        String inputData,
        String expectedOutput,
        Boolean sample,
        Integer sortOrder
) {
}
