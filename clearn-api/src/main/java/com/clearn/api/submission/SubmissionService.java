package com.clearn.api.submission;

import com.clearn.api.problem.Problem;
import com.clearn.api.problem.ProblemMapper;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import com.clearn.api.submission.dto.SubmissionResponse;
import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.enums.UserRole;
import com.clearn.common.judge.JudgeTaskMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SubmissionService {
    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final JudgeTaskPublisher judgeTaskPublisher;
    private final int maxSourceBytes;

    public SubmissionService(
            SubmissionMapper submissionMapper,
            ProblemMapper problemMapper,
            JudgeTaskPublisher judgeTaskPublisher,
            @Value("${clearn.submission.max-source-bytes:65536}") int maxSourceBytes
    ) {
        if (maxSourceBytes <= 0) {
            throw new IllegalArgumentException("clearn.submission.max-source-bytes must be positive");
        }
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.judgeTaskPublisher = judgeTaskPublisher;
        this.maxSourceBytes = maxSourceBytes;
    }

    @Transactional
    public SubmissionCreateResponse createPracticeSubmission(
            Long userId,
            Long problemId,
            SubmissionCreateRequest request
    ) {
        return createSubmission(userId, problemId, null, request, JudgeMode.PRACTICE);
    }

    @Transactional
    public SubmissionCreateResponse createExamSubmission(
            Long userId,
            Long examId,
            Long problemId,
            SubmissionCreateRequest request
    ) {
        requireId(examId, "examId");
        return createSubmission(userId, problemId, examId, request, JudgeMode.EXAM);
    }

    private SubmissionCreateResponse createSubmission(
            Long userId,
            Long problemId,
            Long examId,
            SubmissionCreateRequest request,
            JudgeMode mode
    ) {
        requireId(userId, "userId");
        requireId(problemId, "problemId");
        String sourceCode = validateSourceCode(request);
        requireEnabledProblem(problemId);

        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setExamId(examId);
        submission.setLanguage(Language.C.name());
        submission.setSourceCode(sourceCode);
        submission.setStatus(SubmissionStatus.PENDING.name());
        submission.setScore(0);
        submission.setCreatedAt(createdAt.toLocalDateTime());
        submissionMapper.insert(submission);

        JudgeTaskMessage message = new JudgeTaskMessage(
                submission.getId(),
                problemId,
                Language.C,
                mode,
                examId,
                createdAt
        );
        publishAfterCommit(message);

        return new SubmissionCreateResponse(submission.getId(), submission.getStatus());
    }

    public SubmissionResponse getSubmission(Long currentUserId, UserRole currentRole, Long submissionId) {
        requireId(currentUserId, "currentUserId");
        requireId(submissionId, "submissionId");
        Submission submission = submissionMapper.findById(submissionId);
        if (submission == null || !canView(currentUserId, currentRole, submission)) {
            throw new NoSuchElementException("submission not found");
        }
        return toResponse(submission);
    }

    public List<SubmissionResponse> listMySubmissions(Long currentUserId) {
        requireId(currentUserId, "currentUserId");
        return submissionMapper.findByUserId(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String validateSourceCode(SubmissionCreateRequest request) {
        if (request == null || request.sourceCode() == null || request.sourceCode().isBlank()) {
            throw new IllegalArgumentException("sourceCode must not be blank");
        }
        String sourceCode = request.sourceCode();
        if (sourceCode.getBytes(StandardCharsets.UTF_8).length > maxSourceBytes) {
            throw new IllegalArgumentException("sourceCode exceeds max-source-bytes");
        }
        return sourceCode;
    }

    private void requireEnabledProblem(Long problemId) {
        Problem problem = problemMapper.findEnabledById(problemId);
        if (problem == null) {
            throw new NoSuchElementException("problem not found");
        }
    }

    private boolean canView(Long currentUserId, UserRole currentRole, Submission submission) {
        return currentRole == UserRole.ADMIN || currentUserId.equals(submission.getUserId());
    }

    private void requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private void publishAfterCommit(JudgeTaskMessage message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            judgeTaskPublisher.publish(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                judgeTaskPublisher.publish(message);
            }
        });
    }

    private SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getProblemId(),
                submission.getExamId(),
                submission.getLanguage(),
                submission.getSourceCode(),
                submission.getStatus(),
                submission.getScore(),
                toLong(submission.getTimeUsedMs()),
                toLong(submission.getMemoryUsedKb()),
                submission.getErrorMessage(),
                toOffsetDateTime(submission.getCreatedAt()),
                toOffsetDateTime(submission.getJudgedAt())
        );
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
