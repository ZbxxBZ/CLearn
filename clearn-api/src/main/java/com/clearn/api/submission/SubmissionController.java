package com.clearn.api.submission;

import com.clearn.api.auth.CurrentUser;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import com.clearn.api.submission.dto.SubmissionResponse;
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
@RequestMapping("/api")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/problems/{id}/submissions")
    public ApiResponse<SubmissionCreateResponse> createPracticeSubmission(
            @PathVariable Long id,
            @RequestBody SubmissionCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(submissionService.createPracticeSubmission(currentUser.id(), id, request));
    }

    @GetMapping("/submissions/my")
    public ApiResponse<List<SubmissionResponse>> listMySubmissions(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(submissionService.listMySubmissions(currentUser.id()));
    }

    @GetMapping("/submissions/{id}")
    public ApiResponse<SubmissionResponse> getSubmission(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(submissionService.getSubmission(currentUser.id(), currentUser.role(), id));
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
}
