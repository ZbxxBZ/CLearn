package com.clearn.api.problem;

import com.clearn.api.problem.dto.ProblemCreateRequest;
import com.clearn.api.problem.dto.ProblemDetailResponse;
import com.clearn.api.problem.dto.ProblemImportResponse;
import com.clearn.api.problem.dto.ProblemSummaryResponse;
import com.clearn.api.problem.dto.ProblemUpdateRequest;
import com.clearn.api.problem.dto.TestCaseCreateRequest;
import com.clearn.api.problem.dto.TestCaseResponse;
import com.clearn.api.problem.dto.TestCaseUpdateRequest;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ProblemService {
    private static final int REQUIRED_JUDGE_CASE_COUNT = 5;

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
        ProblemCreateRequest validRequest = validate(request);
        Problem problem = toProblem(validRequest);
        problemMapper.insert(problem);
        replaceProblemTestCases(problem.getId(), validRequest.judgeCases(), validRequest.samples());
        return problem.getId();
    }

    @Transactional
    public ProblemDetailResponse updateProblem(Long id, ProblemUpdateRequest request) {
        if (problemMapper.findById(id) == null) {
            throw new NoSuchElementException("problem not found");
        }
        ProblemUpdateRequest validRequest = validate(request);
        Problem problem = toProblem(validRequest);
        problem.setId(id);
        problemMapper.update(problem);
        if (validRequest.judgeCases() != null || validRequest.samples() != null) {
            replaceProblemTestCases(id, validRequest.judgeCases(), validRequest.samples());
        }
        return toDetailResponse(problemMapper.findById(id), testCaseMapper.findSamplesByProblemId(id));
    }

    @Transactional
    public ProblemImportResponse importProblems(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file must not be empty");
        }

        List<ProblemCreateRequest> requests = parseProblemWorkbook(file);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Excel file must contain at least one problem row");
        }

        List<Long> ids = new ArrayList<>();
        for (ProblemCreateRequest request : requests) {
            ids.add(createProblem(request));
        }
        return new ProblemImportResponse(ids.size(), ids);
    }

    @Transactional
    public TestCaseResponse addTestCase(Long problemId, TestCaseCreateRequest request) {
        if (problemMapper.findById(problemId) == null) {
            throw new NoSuchElementException("problem not found");
        }
        TestCase testCase = toTestCase(problemId, validate(request));
        requireSortOrderAvailable(problemId, testCase.getSortOrder());
        if (!Boolean.TRUE.equals(testCase.getSample())
                && testCaseMapper.countJudgeCasesByProblemId(problemId) >= REQUIRED_JUDGE_CASE_COUNT) {
            throw new IllegalArgumentException("problem must contain exactly 5 judge test cases");
        }
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
        if (!Boolean.TRUE.equals(existing.getSample()) && Boolean.TRUE.equals(updated.getSample())) {
            throw new IllegalArgumentException("problem must contain exactly 5 judge test cases");
        }
        if (Boolean.TRUE.equals(existing.getSample())
                && !Boolean.TRUE.equals(updated.getSample())
                && testCaseMapper.countJudgeCasesByProblemId(existing.getProblemId()) >= REQUIRED_JUDGE_CASE_COUNT) {
            throw new IllegalArgumentException("problem must contain exactly 5 judge test cases");
        }
        try {
            testCaseMapper.update(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new TestCaseSortOrderConflictException(ex);
        }
        return toTestCaseResponse(testCaseMapper.findById(id));
    }

    @Transactional
    public void deleteTestCase(Long id) {
        TestCase existing = testCaseMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("test case not found");
        }
        if (!Boolean.TRUE.equals(existing.getSample())) {
            throw new IllegalArgumentException("problem must contain exactly 5 judge test cases");
        }
        testCaseMapper.deleteById(id);
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
        validateJudgeCases(request.judgeCases());
        validateSampleCases(request.samples());
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
        if (request.samples() != null) {
            if (request.judgeCases() == null) {
                throw new IllegalArgumentException("judgeCases must be provided when replacing test cases");
            }
            validateSampleCases(request.samples());
        }
        if (request.judgeCases() != null) {
            validateJudgeCases(request.judgeCases());
        }
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

    private void validateJudgeCases(List<TestCaseCreateRequest> judgeCases) {
        if (judgeCases == null || judgeCases.size() != REQUIRED_JUDGE_CASE_COUNT) {
            throw new IllegalArgumentException("problem must contain exactly 5 judge test cases");
        }
        for (int index = 0; index < judgeCases.size(); index++) {
            TestCaseCreateRequest testCase = judgeCases.get(index);
            validateTestCaseFields(testCase.inputData(), testCase.expectedOutput(), normalizedSortOrder(testCase.sortOrder(), index + 1));
        }
    }

    private void validateSampleCases(List<TestCaseCreateRequest> samples) {
        if (samples == null) {
            return;
        }
        for (int index = 0; index < samples.size(); index++) {
            TestCaseCreateRequest sample = samples.get(index);
            validateTestCaseFields(sample.inputData(), sample.expectedOutput(), normalizedSortOrder(sample.sortOrder(), 1000 + index + 1));
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

    private void replaceProblemTestCases(
            Long problemId,
            List<TestCaseCreateRequest> judgeCases,
            List<TestCaseCreateRequest> samples
    ) {
        testCaseMapper.deleteByProblemId(problemId);
        insertCases(problemId, judgeCases, false, 1);
        insertCases(problemId, samples == null ? List.of() : samples, true, 1001);
    }

    private void insertCases(
            Long problemId,
            List<TestCaseCreateRequest> cases,
            boolean sample,
            int defaultSortOrderStart
    ) {
        for (int index = 0; index < cases.size(); index++) {
            TestCaseCreateRequest request = cases.get(index);
            TestCase testCase = new TestCase();
            testCase.setProblemId(problemId);
            testCase.setInputData(request.inputData());
            testCase.setExpectedOutput(request.expectedOutput());
            testCase.setSample(sample);
            testCase.setSortOrder(normalizedSortOrder(request.sortOrder(), defaultSortOrderStart + index));
            try {
                testCaseMapper.insert(testCase);
            } catch (DataIntegrityViolationException ex) {
                throw new TestCaseSortOrderConflictException(ex);
            }
        }
    }

    private int normalizedSortOrder(Integer sortOrder, int fallback) {
        return sortOrder == null ? fallback : sortOrder;
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

    private List<ProblemCreateRequest> parseProblemWorkbook(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = readHeader(sheet.getRow(0));
            List<ProblemCreateRequest> requests = new ArrayList<>();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                requests.add(toImportRequest(row, columns, formatter, rowIndex + 1));
            }
            return requests;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read Excel file", ex);
        }
    }

    private Map<String, Integer> readHeader(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Excel header row is required");
        }
        Map<String, Integer> columns = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (int index = 0; index < row.getLastCellNum(); index++) {
            String header = formatter.formatCellValue(row.getCell(index)).trim();
            if (!header.isBlank()) {
                columns.put(header, index);
            }
        }
        return columns;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int index = 0; index < row.getLastCellNum(); index++) {
            if (!formatter.formatCellValue(row.getCell(index)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private ProblemCreateRequest toImportRequest(
            Row row,
            Map<String, Integer> columns,
            DataFormatter formatter,
            int rowNumber
    ) {
        List<TestCaseCreateRequest> judgeCases = new ArrayList<>();
        for (int index = 1; index <= REQUIRED_JUDGE_CASE_COUNT; index++) {
            judgeCases.add(new TestCaseCreateRequest(
                    requiredCell(row, columns, formatter, rowNumber, "case" + index + "Input"),
                    requiredCell(row, columns, formatter, rowNumber, "case" + index + "Output"),
                    false,
                    index
            ));
        }

        List<TestCaseCreateRequest> samples = new ArrayList<>();
        String sampleInput = optionalCell(row, columns, formatter, "sampleInput");
        String sampleOutput = optionalCell(row, columns, formatter, "sampleOutput");
        if (!sampleInput.isBlank() || !sampleOutput.isBlank()) {
            if (sampleInput.isBlank() || sampleOutput.isBlank()) {
                throw new IllegalArgumentException("Excel row " + rowNumber + " sample input and output must be provided together");
            }
            samples.add(new TestCaseCreateRequest(sampleInput, sampleOutput, true, 1001));
        }

        return new ProblemCreateRequest(
                requiredCell(row, columns, formatter, rowNumber, "title"),
                requiredCell(row, columns, formatter, rowNumber, "description"),
                optionalCell(row, columns, formatter, "inputDescription"),
                optionalCell(row, columns, formatter, "outputDescription"),
                optionalCell(row, columns, formatter, "difficulty").isBlank()
                        ? "EASY"
                        : optionalCell(row, columns, formatter, "difficulty"),
                optionalCell(row, columns, formatter, "tags"),
                parseInteger(optionalCell(row, columns, formatter, "timeLimitMs"), 1000, "timeLimitMs", rowNumber),
                parseInteger(optionalCell(row, columns, formatter, "memoryLimitMb"), 128, "memoryLimitMb", rowNumber),
                parseInteger(optionalCell(row, columns, formatter, "score"), 100, "score", rowNumber),
                parseBoolean(optionalCell(row, columns, formatter, "enabled"), true),
                judgeCases,
                samples
        );
    }

    private String requiredCell(
            Row row,
            Map<String, Integer> columns,
            DataFormatter formatter,
            int rowNumber,
            String column
    ) {
        String value = optionalCell(row, columns, formatter, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Excel row " + rowNumber + " column " + column + " must not be blank");
        }
        return value;
    }

    private String optionalCell(Row row, Map<String, Integer> columns, DataFormatter formatter, String column) {
        Integer index = columns.get(column);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private int parseInteger(String value, int fallback, String column, int rowNumber) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Excel row " + rowNumber + " column " + column + " must be an integer");
        }
    }

    private boolean parseBoolean(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }
}
