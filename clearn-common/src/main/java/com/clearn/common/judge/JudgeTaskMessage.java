package com.clearn.common.judge;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import java.time.OffsetDateTime;

public record JudgeTaskMessage(
        Long submissionId,
        Long problemId,
        Language language,
        JudgeMode mode,
        Long examId,
        OffsetDateTime createdAt
) {
}
