package com.clearn.api.exam;

import com.clearn.api.exam.dto.ExamAdminResponse;
import com.clearn.api.exam.dto.ExamCreateRequest;
import com.clearn.api.exam.dto.ExamDetailResponse;
import com.clearn.api.exam.dto.ExamProblemResponse;
import com.clearn.api.exam.dto.ExamProblemResultResponse;
import com.clearn.api.exam.dto.ExamProblemCreateRequest;
import com.clearn.api.exam.dto.ExamResultResponse;
import com.clearn.api.exam.dto.ExamResultsResponse;
import com.clearn.api.exam.dto.ExamStudentResultResponse;
import com.clearn.api.exam.dto.ExamSummaryResponse;
import com.clearn.api.exam.dto.ExamUpdateRequest;
import com.clearn.api.problem.ProblemMapper;
import com.clearn.api.submission.SubmissionService;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ExamService {
    private final ExamMapper examMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionService submissionService;

    public ExamService(ExamMapper examMapper, ProblemMapper problemMapper, SubmissionService submissionService) {
        this.examMapper = examMapper;
        this.problemMapper = problemMapper;
        this.submissionService = submissionService;
    }

    public List<ExamSummaryResponse> listOpenExams() {
        return examMapper.findOpenExams(nowUtc())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public ExamDetailResponse getExamDetail(Long examId) {
        requireId(examId, "examId");
        Exam exam = requireEnabledExam(examId);
        requireOpen(exam);
        return toDetailResponse(exam, examMapper.findEnabledProblemsByExamId(examId));
    }

    public SubmissionCreateResponse createExamSubmission(
            Long userId,
            Long examId,
            Long problemId,
            SubmissionCreateRequest request
    ) {
        requireId(userId, "userId");
        requireId(examId, "examId");
        requireId(problemId, "problemId");
        Exam exam = requireEnabledExam(examId);
        requireOpen(exam);
        requireExamProblem(examId, problemId);
        return submissionService.createExamSubmission(userId, examId, problemId, request);
    }

    public ExamResultResponse getMyResult(Long userId, Long examId) {
        requireId(userId, "userId");
        requireId(examId, "examId");
        Exam exam = requireEnabledExam(examId);
        List<ExamProblemResultResponse> problems = examMapper.findMyProblemScores(examId, userId)
                .stream()
                .map(this::toProblemResult)
                .toList();
        int totalScore = problems.stream()
                .mapToInt(ExamProblemResultResponse::score)
                .sum();
        int maxScore = problems.stream()
                .mapToInt(ExamProblemResultResponse::maxScore)
                .sum();
        return new ExamResultResponse(exam.getId(), exam.getTitle(), totalScore, maxScore, problems);
    }

    public List<ExamAdminResponse> listAdminExams() {
        return examMapper.findAll()
                .stream()
                .map(exam -> toAdminResponse(exam, examMapper.findEnabledProblemsByExamId(exam.getId())))
                .toList();
    }

    public ExamAdminResponse getAdminExam(Long examId) {
        requireId(examId, "examId");
        Exam exam = requireExam(examId);
        return toAdminResponse(exam, examMapper.findEnabledProblemsByExamId(examId));
    }

    @Transactional
    public ExamAdminResponse createExam(ExamCreateRequest request) {
        Exam exam = toExam(validate(request));
        examMapper.insert(exam);
        return toAdminResponse(examMapper.findById(exam.getId()), List.of());
    }

    @Transactional
    public ExamAdminResponse updateExam(Long examId, ExamUpdateRequest request) {
        requireId(examId, "examId");
        if (examMapper.findById(examId) == null) {
            throw new NoSuchElementException("exam not found");
        }
        Exam exam = toExam(validate(request));
        exam.setId(examId);
        examMapper.update(exam);
        return getAdminExam(examId);
    }

    @Transactional
    public void disableExam(Long examId) {
        requireId(examId, "examId");
        if (examMapper.disableById(examId) == 0) {
            throw new NoSuchElementException("exam not found");
        }
    }

    @Transactional
    public ExamProblemResponse addExamProblem(Long examId, ExamProblemCreateRequest request) {
        requireId(examId, "examId");
        if (examMapper.findById(examId) == null) {
            throw new NoSuchElementException("exam not found");
        }
        ExamProblem examProblem = toExamProblem(examId, validate(request));
        if (problemMapper.findEnabledById(examProblem.getProblemId()) == null) {
            throw new NoSuchElementException("problem not found");
        }
        try {
            examMapper.insertExamProblem(examProblem);
        } catch (DataIntegrityViolationException ex) {
            throw new ExamProblemConflictException(ex);
        }
        return examMapper.findEnabledProblemsByExamId(examId)
                .stream()
                .filter(problem -> problem.getProblemId().equals(examProblem.getProblemId()))
                .findFirst()
                .map(this::toProblemResponse)
                .orElseGet(() -> toProblemResponse(examProblem));
    }

    public ExamResultsResponse getExamResults(Long examId) {
        requireId(examId, "examId");
        Exam exam = requireExam(examId);
        List<ExamProblem> examProblems = examMapper.findEnabledProblemsByExamId(examId);
        int maxScore = examProblems.stream()
                .mapToInt(problem -> valueOrZero(problem.getScore()))
                .sum();

        Map<Long, MutableStudentResult> students = new LinkedHashMap<>();
        for (ExamProblemResultRow row : examMapper.findExamResultRows(examId)) {
            MutableStudentResult student = students.computeIfAbsent(
                    row.getUserId(),
                    ignored -> new MutableStudentResult(row.getUserId(), row.getUsername())
            );
            student.problems().add(toProblemResult(row));
        }

        List<ExamStudentResultResponse> studentResponses = students.values()
                .stream()
                .map(this::toStudentResult)
                .toList();
        return new ExamResultsResponse(exam.getId(), exam.getTitle(), maxScore, studentResponses);
    }

    private Exam requireEnabledExam(Long examId) {
        Exam exam = examMapper.findEnabledById(examId);
        if (exam == null) {
            throw new NoSuchElementException("exam not found");
        }
        return exam;
    }

    private Exam requireExam(Long examId) {
        Exam exam = examMapper.findById(examId);
        if (exam == null) {
            throw new NoSuchElementException("exam not found");
        }
        return exam;
    }

    private void requireOpen(Exam exam) {
        LocalDateTime now = nowUtc();
        if (now.isBefore(exam.getStartTime()) || now.isAfter(exam.getEndTime())) {
            throw new ExamClosedException();
        }
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void requireExamProblem(Long examId, Long problemId) {
        if (examMapper.countEnabledExamProblem(examId, problemId) == 0) {
            throw new NoSuchElementException("exam problem not found");
        }
    }

    private void requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private ExamCreateRequest validate(ExamCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("exam request must not be null");
        }
        validateExamFields(request.title(), request.startTime(), request.endTime());
        return request;
    }

    private ExamUpdateRequest validate(ExamUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("exam request must not be null");
        }
        validateExamFields(request.title(), request.startTime(), request.endTime());
        return request;
    }

    private ExamProblemCreateRequest validate(ExamProblemCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("exam problem request must not be null");
        }
        requireId(request.problemId(), "problemId");
        if (request.score() == null || request.score() <= 0) {
            throw new IllegalArgumentException("score must be positive");
        }
        if (request.sortOrder() == null || request.sortOrder() < 0) {
            throw new IllegalArgumentException("sortOrder must be zero or positive");
        }
        return request;
    }

    private void validateExamFields(String title, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime must not be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    private Exam toExam(ExamCreateRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.title().trim());
        exam.setDescription(blankToEmpty(request.description()));
        exam.setStartTime(toUtcLocalDateTime(request.startTime()));
        exam.setEndTime(toUtcLocalDateTime(request.endTime()));
        exam.setEnabled(request.enabled() == null || request.enabled());
        return exam;
    }

    private Exam toExam(ExamUpdateRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.title().trim());
        exam.setDescription(blankToEmpty(request.description()));
        exam.setStartTime(toUtcLocalDateTime(request.startTime()));
        exam.setEndTime(toUtcLocalDateTime(request.endTime()));
        exam.setEnabled(request.enabled() == null || request.enabled());
        return exam;
    }

    private ExamProblem toExamProblem(Long examId, ExamProblemCreateRequest request) {
        ExamProblem examProblem = new ExamProblem();
        examProblem.setExamId(examId);
        examProblem.setProblemId(request.problemId());
        examProblem.setScore(request.score());
        examProblem.setSortOrder(request.sortOrder());
        return examProblem;
    }

    private ExamSummaryResponse toSummaryResponse(Exam exam) {
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                toOffsetDateTime(exam.getStartTime()),
                toOffsetDateTime(exam.getEndTime())
        );
    }

    private ExamDetailResponse toDetailResponse(Exam exam, List<ExamProblem> problems) {
        return new ExamDetailResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                toOffsetDateTime(exam.getStartTime()),
                toOffsetDateTime(exam.getEndTime()),
                problems.stream().map(this::toProblemResponse).toList()
        );
    }

    private ExamAdminResponse toAdminResponse(Exam exam, List<ExamProblem> problems) {
        return new ExamAdminResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                toOffsetDateTime(exam.getStartTime()),
                toOffsetDateTime(exam.getEndTime()),
                exam.getEnabled(),
                problems.stream().map(this::toProblemResponse).toList()
        );
    }

    private ExamProblemResponse toProblemResponse(ExamProblem problem) {
        return new ExamProblemResponse(
                problem.getProblemId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getScore(),
                problem.getSortOrder()
        );
    }

    private ExamProblemResultResponse toProblemResult(ExamProblemScoreRow row) {
        return new ExamProblemResultResponse(
                row.getProblemId(),
                row.getTitle(),
                valueOrZero(row.getScore()),
                valueOrZero(row.getMaxScore()),
                row.getBestSubmissionId(),
                row.getBestStatus()
        );
    }

    private ExamProblemResultResponse toProblemResult(ExamProblemResultRow row) {
        return new ExamProblemResultResponse(
                row.getProblemId(),
                row.getTitle(),
                valueOrZero(row.getScore()),
                valueOrZero(row.getMaxScore()),
                row.getBestSubmissionId(),
                row.getBestStatus()
        );
    }

    private ExamStudentResultResponse toStudentResult(MutableStudentResult result) {
        int totalScore = result.problems()
                .stream()
                .mapToInt(ExamProblemResultResponse::score)
                .sum();
        return new ExamStudentResultResponse(
                result.userId(),
                result.username(),
                totalScore,
                result.problems()
        );
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime value) {
        return value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record MutableStudentResult(
            Long userId,
            String username,
            List<ExamProblemResultResponse> problems
    ) {
        private MutableStudentResult(Long userId, String username) {
            this(userId, username, new ArrayList<>());
        }
    }
}
