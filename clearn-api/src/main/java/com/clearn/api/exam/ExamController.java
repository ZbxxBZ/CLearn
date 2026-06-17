package com.clearn.api.exam;

import com.clearn.api.auth.CurrentUser;
import com.clearn.api.exam.dto.ExamDetailResponse;
import com.clearn.api.exam.dto.ExamResultResponse;
import com.clearn.api.exam.dto.ExamSummaryResponse;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import com.clearn.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<List<ExamSummaryResponse>> listOpenExams() {
        return ApiResponse.success(examService.listOpenExams());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamDetailResponse> getExam(@PathVariable Long id) {
        return ApiResponse.success(examService.getExamDetail(id));
    }

    @PostMapping("/{examId}/problems/{problemId}/submissions")
    public ApiResponse<SubmissionCreateResponse> createExamSubmission(
            @PathVariable Long examId,
            @PathVariable Long problemId,
            @RequestBody SubmissionCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(examService.createExamSubmission(currentUser.id(), examId, problemId, request));
    }

    @GetMapping("/{id}/my-result")
    public ApiResponse<ExamResultResponse> getMyResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(examService.getMyResult(currentUser.id(), id));
    }

    @ExceptionHandler({IllegalArgumentException.class, ExamClosedException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
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
}
