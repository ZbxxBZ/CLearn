package com.clearn.api.exam.dto;

import java.util.List;

public record ExamResultResponse(
        Long examId,
        String title,
        Integer totalScore,
        Integer maxScore,
        List<ExamProblemResultResponse> problems
) {
}
