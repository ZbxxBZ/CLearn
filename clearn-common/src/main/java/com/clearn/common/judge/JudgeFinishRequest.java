package com.clearn.common.judge;

import com.clearn.common.enums.SubmissionStatus;

public record JudgeFinishRequest(
        SubmissionStatus status,
        Integer score,
        Integer passedTestCases,
        Integer totalTestCases,
        Long timeUsedMs,
        Long memoryUsedKb,
        String errorMessage
) {
}
