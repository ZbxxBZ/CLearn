package com.clearn.api.exam.dto;

import java.util.List;

public record ExamResultsResponse(
        Long examId,
        String title,
        Integer maxScore,
        List<ExamStudentResultResponse> students
) {
}
