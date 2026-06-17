package com.clearn.api.problem;

import com.clearn.api.problem.dto.ProblemCreateRequest;
import com.clearn.api.problem.dto.ProblemDetailResponse;
import com.clearn.api.problem.dto.ProblemSummaryResponse;
import com.clearn.api.problem.dto.ProblemUpdateRequest;
import com.clearn.api.problem.dto.TestCaseCreateRequest;
import com.clearn.api.problem.dto.TestCaseResponse;
import com.clearn.api.problem.dto.TestCaseUpdateRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProblemService {
    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;

    public ProblemService(ProblemMapper problemMapper, TestCaseMapper testCaseMapper) {
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
    }

    public List<ProblemSummaryResponse> listEnabledProblems() {
        return problemMapper.findEnabledProblems()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public ProblemDetailResponse getStudentProblem(Long id) {
        Problem problem = problemMapper.findEnabledById(id);
        if (problem == null) {
            throw new NoSuchElementException("problem not found");
        }
        return toDetailResponse(problem, testCaseMapper.findSamplesByProblemId(id));
    }

    @Transactional
    public Long createProblem(ProblemCreateRequest request) {
        Problem problem = toProblem(validate(request));
        problemMapper.insert(problem);
        return problem.getId();
    }

    @Transactional
    public ProblemDetailResponse updateProblem(Long id, ProblemUpdateRequest request) {
        if (problemMapper.findById(id) == null) {
            throw new NoSuchElementException("problem not found");
        }
        Problem problem = toProblem(validate(request));
        problem.setId(id);
        problemMapper.update(problem);
        return toDetailResponse(problemMapper.findById(id), testCaseMapper.findSamplesByProblemId(id));
    }

    @Transactional
    public TestCaseResponse addTestCase(Long problemId, TestCaseCreateRequest request) {
        if (problemMapper.findById(problemId) == null) {
            throw new NoSuchElementException("problem not found");
        }
        TestCase testCase = toTestCase(problemId, validate(request));
        requireSortOrderAvailable(problemId, testCase.getSortOrder());
        try {
            testCaseMapper.insert(testCase);
        } catch (DataIntegrityViolationException ex) {
            throw new TestCaseSortOrderConflictException(ex);
        }
        return toTestCaseResponse(testCase);
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long id, TestCaseUpdateRequest request) {
        TestCase existing = testCaseMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("test case not found");
        }
        TestCase updated = toTestCase(existing.getProblemId(), validate(request));
        updated.setId(id);
        requireSortOrderAvailable(existing.getProblemId(), updated.getSortOrder(), id);
        try {
            testCaseMapper.update(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new TestCaseSortOrderConflictException(ex);
        }
        return toTestCaseResponse(testCaseMapper.findById(id));
    }

    @Transactional
    public void deleteTestCase(Long id) {
        if (testCaseMapper.deleteById(id) == 0) {
            throw new NoSuchElementException("test case not found");
        }
    }

    private ProblemCreateRequest validate(ProblemCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("problem request must not be null");
        }
        validateProblemFields(
                request.title(),
                request.description(),
                request.difficulty(),
                request.timeLimitMs(),
                request.memoryLimitMb(),
                request.score()
        );
        return request;
    }

    private ProblemUpdateRequest validate(ProblemUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("problem request must not be null");
        }
        validateProblemFields(
                request.title(),
                request.description(),
                request.difficulty(),
                request.timeLimitMs(),
                request.memoryLimitMb(),
                request.score()
        );
        return request;
    }

    private TestCaseCreateRequest validate(TestCaseCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("test case request must not be null");
        }
        validateTestCaseFields(request.inputData(), request.expectedOutput(), request.sortOrder());
        return request;
    }

    private TestCaseUpdateRequest validate(TestCaseUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("test case request must not be null");
        }
        validateTestCaseFields(request.inputData(), request.expectedOutput(), request.sortOrder());
        return request;
    }

    private void validateProblemFields(
            String title,
            String description,
            String difficulty,
            Integer timeLimitMs,
            Integer memoryLimitMb,
            Integer score
    ) {
        requireText(title, "title");
        requireText(description, "description");
        requireText(difficulty, "difficulty");
        requirePositive(timeLimitMs, "timeLimitMs");
        requirePositive(memoryLimitMb, "memoryLimitMb");
        if (score == null || score < 0) {
            throw new IllegalArgumentException("score must be zero or positive");
        }
    }

    private void validateTestCaseFields(String inputData, String expectedOutput, Integer sortOrder) {
        if (inputData == null) {
            throw new IllegalArgumentException("inputData must not be null");
        }
        if (expectedOutput == null) {
            throw new IllegalArgumentException("expectedOutput must not be null");
        }
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be zero or positive");
        }
    }

    private void requireSortOrderAvailable(Long problemId, Integer sortOrder) {
        if (testCaseMapper.countByProblemIdAndSortOrder(problemId, sortOrder) > 0) {
            throw new TestCaseSortOrderConflictException();
        }
    }

    private void requireSortOrderAvailable(Long problemId, Integer sortOrder, Long excludingId) {
        if (testCaseMapper.countByProblemIdAndSortOrderExcludingId(problemId, sortOrder, excludingId) > 0) {
            throw new TestCaseSortOrderConflictException();
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private Problem toProblem(ProblemCreateRequest request) {
        Problem problem = new Problem();
        problem.setTitle(request.title().trim());
        problem.setDescription(request.description().trim());
        problem.setInputDescription(blankToEmpty(request.inputDescription()));
        problem.setOutputDescription(blankToEmpty(request.outputDescription()));
        problem.setDifficulty(request.difficulty().trim());
        problem.setTags(blankToEmpty(request.tags()));
        problem.setTimeLimitMs(request.timeLimitMs());
        problem.setMemoryLimitMb(request.memoryLimitMb());
        problem.setScore(request.score());
        problem.setEnabled(request.enabled() == null || request.enabled());
        return problem;
    }

    private Problem toProblem(ProblemUpdateRequest request) {
        Problem problem = new Problem();
        problem.setTitle(request.title().trim());
        problem.setDescription(request.description().trim());
        problem.setInputDescription(blankToEmpty(request.inputDescription()));
        problem.setOutputDescription(blankToEmpty(request.outputDescription()));
        problem.setDifficulty(request.difficulty().trim());
        problem.setTags(blankToEmpty(request.tags()));
        problem.setTimeLimitMs(request.timeLimitMs());
        problem.setMemoryLimitMb(request.memoryLimitMb());
        problem.setScore(request.score());
        problem.setEnabled(request.enabled() == null || request.enabled());
        return problem;
    }

    private TestCase toTestCase(Long problemId, TestCaseCreateRequest request) {
        TestCase testCase = new TestCase();
        testCase.setProblemId(problemId);
        testCase.setInputData(request.inputData());
        testCase.setExpectedOutput(request.expectedOutput());
        testCase.setSample(Boolean.TRUE.equals(request.sample()));
        testCase.setSortOrder(request.sortOrder());
        return testCase;
    }

    private TestCase toTestCase(Long problemId, TestCaseUpdateRequest request) {
        TestCase testCase = new TestCase();
        testCase.setProblemId(problemId);
        testCase.setInputData(request.inputData());
        testCase.setExpectedOutput(request.expectedOutput());
        testCase.setSample(Boolean.TRUE.equals(request.sample()));
        testCase.setSortOrder(request.sortOrder());
        return testCase;
    }

    private ProblemSummaryResponse toSummaryResponse(Problem problem) {
        return new ProblemSummaryResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getTags(),
                problem.getScore()
        );
    }

    private ProblemDetailResponse toDetailResponse(Problem problem, List<TestCase> samples) {
        return new ProblemDetailResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getInputDescription(),
                problem.getOutputDescription(),
                problem.getDifficulty(),
                problem.getTags(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitMb(),
                problem.getScore(),
                samples.stream().map(this::toTestCaseResponse).toList()
        );
    }

    private TestCaseResponse toTestCaseResponse(TestCase testCase) {
        return new TestCaseResponse(
                testCase.getId(),
                testCase.getInputData(),
                testCase.getExpectedOutput(),
                Boolean.TRUE.equals(testCase.getSample()),
                testCase.getSortOrder()
        );
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
