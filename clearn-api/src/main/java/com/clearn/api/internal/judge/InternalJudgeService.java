package com.clearn.api.internal.judge;

import com.clearn.api.submission.Submission;
import com.clearn.api.submission.SubmissionMapper;
import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.NoSuchElementException;

@Service
public class InternalJudgeService {
    private static final EnumSet<SubmissionStatus> TERMINAL_STATUSES = EnumSet.of(
            SubmissionStatus.AC,
            SubmissionStatus.WA,
            SubmissionStatus.CE,
            SubmissionStatus.TLE,
            SubmissionStatus.MLE,
            SubmissionStatus.RE,
            SubmissionStatus.SE
    );

    private final SubmissionMapper submissionMapper;

    public InternalJudgeService(SubmissionMapper submissionMapper) {
        this.submissionMapper = submissionMapper;
    }

    @Transactional
    public InternalJudgeResponse start(Long submissionId) {
        requireId(submissionId);
        requireSubmissionForUpdate(submissionId);
        submissionMapper.markJudgingIfPending(submissionId);
        return toResponse(requireSubmissionForUpdate(submissionId));
    }

    @Transactional
    public InternalJudgeResponse finish(Long submissionId, JudgeFinishRequest request) {
        requireId(submissionId);
        JudgeFinishRequest validRequest = validateFinishRequest(request);
        requireSubmissionForUpdate(submissionId);
        submissionMapper.finishIfJudging(
                submissionId,
                validRequest.status().name(),
                defaultScore(validRequest.score()),
                defaultCount(validRequest.passedTestCases()),
                defaultCount(validRequest.totalTestCases()),
                toInteger(validRequest.timeUsedMs(), "timeUsedMs"),
                toInteger(validRequest.memoryUsedKb(), "memoryUsedKb"),
                validRequest.errorMessage(),
                LocalDateTime.now()
        );
        return toResponse(requireSubmissionForUpdate(submissionId));
    }

    @Transactional
    public InternalJudgeResponse systemError(Long submissionId, JudgeSystemErrorRequest request) {
        requireId(submissionId);
        JudgeSystemErrorRequest validRequest = validateSystemErrorRequest(request);
        requireSubmissionForUpdate(submissionId);
        submissionMapper.markSystemErrorIfOpen(
                submissionId,
                validRequest.errorMessage(),
                LocalDateTime.now()
        );
        return toResponse(requireSubmissionForUpdate(submissionId));
    }

    private JudgeFinishRequest validateFinishRequest(JudgeFinishRequest request) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (!TERMINAL_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("finish status must be terminal");
        }
        requireNonNegative(request.score(), "score");
        requireNonNegative(request.passedTestCases(), "passedTestCases");
        requireNonNegative(request.totalTestCases(), "totalTestCases");
        if (request.passedTestCases() != null
                && request.totalTestCases() != null
                && request.passedTestCases() > request.totalTestCases()) {
            throw new IllegalArgumentException("passedTestCases must not exceed totalTestCases");
        }
        requireNonNegative(request.timeUsedMs(), "timeUsedMs");
        requireNonNegative(request.memoryUsedKb(), "memoryUsedKb");
        return request;
    }

    private JudgeSystemErrorRequest validateSystemErrorRequest(JudgeSystemErrorRequest request) {
        if (request == null || request.errorMessage() == null || request.errorMessage().isBlank()) {
            throw new IllegalArgumentException("errorMessage must not be blank");
        }
        return request;
    }

    private Submission requireSubmissionForUpdate(Long submissionId) {
        Submission submission = submissionMapper.findByIdForUpdate(submissionId);
        if (submission == null) {
            throw new NoSuchElementException("submission not found");
        }
        return submission;
    }

    private void requireId(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("submissionId must not be null");
        }
    }

    private int defaultScore(Integer score) {
        return score == null ? 0 : score;
    }

    private int defaultCount(Integer count) {
        return count == null ? 0 : count;
    }

    private Integer toInteger(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " exceeds integer range");
        }
        return value.intValue();
    }

    private void requireNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
    }

    private void requireNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
    }

    private InternalJudgeResponse toResponse(Submission submission) {
        return new InternalJudgeResponse(submission.getId(), submission.getStatus());
    }
}
