package com.clearn.api.problem.dto;

public record TestCaseUpdateRequest(
        String inputData,
        String expectedOutput,
        Boolean sample,
        Integer sortOrder
) {
}
