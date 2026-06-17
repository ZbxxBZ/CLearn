package com.clearn.api.exam.dto;

import java.util.List;

public record ExamStudentResultResponse(
        Long userId,
        String username,
        Integer totalScore,
        List<ExamProblemResultResponse> problems
) {
}
