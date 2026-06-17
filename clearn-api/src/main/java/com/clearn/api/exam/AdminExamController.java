package com.clearn.api.exam;

import com.clearn.api.exam.dto.ExamAdminResponse;
import com.clearn.api.exam.dto.ExamCreateRequest;
import com.clearn.api.exam.dto.ExamProblemCreateRequest;
import com.clearn.api.exam.dto.ExamProblemResponse;
import com.clearn.api.exam.dto.ExamResultsResponse;
import com.clearn.api.exam.dto.ExamUpdateRequest;
import com.clearn.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin")
public class AdminExamController {
    private final ExamService examService;

    public AdminExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping("/exams")
    public ApiResponse<List<ExamAdminResponse>> listExams() {
        return ApiResponse.success(examService.listAdminExams());
    }

    @PostMapping("/exams")
    public ApiResponse<ExamAdminResponse> createExam(@RequestBody ExamCreateRequest request) {
        return ApiResponse.success(examService.createExam(request));
    }

    @GetMapping("/exams/{id}")
    public ApiResponse<ExamAdminResponse> getExam(@PathVariable Long id) {
        return ApiResponse.success(examService.getAdminExam(id));
    }

    @PutMapping("/exams/{id}")
    public ApiResponse<ExamAdminResponse> updateExam(
            @PathVariable Long id,
            @RequestBody ExamUpdateRequest request
    ) {
        return ApiResponse.success(examService.updateExam(id, request));
    }

    @DeleteMapping("/exams/{id}")
    public ApiResponse<Void> disableExam(@PathVariable Long id) {
        examService.disableExam(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/exams/{id}/problems")
    public ApiResponse<ExamProblemResponse> addExamProblem(
            @PathVariable Long id,
            @RequestBody ExamProblemCreateRequest request
    ) {
        return ApiResponse.success(examService.addExamProblem(id, request));
    }

    @GetMapping("/exams/{id}/results")
    public ApiResponse<ExamResultsResponse> getExamResults(@PathVariable Long id) {
        return ApiResponse.success(examService.getExamResults(id));
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

    @ExceptionHandler(ExamProblemConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ExamProblemConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage()));
    }
}
