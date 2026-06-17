package com.clearn.api.problem;

import com.clearn.api.problem.dto.ProblemCreateRequest;
import com.clearn.api.problem.dto.ProblemDetailResponse;
import com.clearn.api.problem.dto.ProblemUpdateRequest;
import com.clearn.api.problem.dto.TestCaseCreateRequest;
import com.clearn.api.problem.dto.TestCaseResponse;
import com.clearn.api.problem.dto.TestCaseUpdateRequest;
import com.clearn.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminProblemController {
    private final ProblemService problemService;

    public AdminProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @PostMapping("/problems")
    public ApiResponse<Long> createProblem(@RequestBody ProblemCreateRequest request) {
        return ApiResponse.success(problemService.createProblem(request));
    }

    @PutMapping("/problems/{id}")
    public ApiResponse<ProblemDetailResponse> updateProblem(
            @PathVariable Long id,
            @RequestBody ProblemUpdateRequest request
    ) {
        return ApiResponse.success(problemService.updateProblem(id, request));
    }

    @PostMapping("/problems/{id}/test-cases")
    public ApiResponse<TestCaseResponse> addTestCase(
            @PathVariable Long id,
            @RequestBody TestCaseCreateRequest request
    ) {
        return ApiResponse.success(problemService.addTestCase(id, request));
    }

    @PutMapping("/test-cases/{id}")
    public ApiResponse<TestCaseResponse> updateTestCase(
            @PathVariable Long id,
            @RequestBody TestCaseUpdateRequest request
    ) {
        return ApiResponse.success(problemService.updateTestCase(id, request));
    }

    @DeleteMapping("/test-cases/{id}")
    public ApiResponse<Void> deleteTestCase(@PathVariable Long id) {
        problemService.deleteTestCase(id);
        return ApiResponse.success(null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(TestCaseSortOrderConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(TestCaseSortOrderConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage()));
    }
}
