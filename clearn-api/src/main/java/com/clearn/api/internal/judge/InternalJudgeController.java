package com.clearn.api.internal.judge;

import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import com.clearn.common.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/internal/judge/submissions")
public class InternalJudgeController {
    private final InternalJudgeService internalJudgeService;

    public InternalJudgeController(InternalJudgeService internalJudgeService) {
        this.internalJudgeService = internalJudgeService;
    }

    @PostMapping("/{id}/start")
    public ApiResponse<InternalJudgeResponse> start(@PathVariable Long id) {
        return ApiResponse.success(internalJudgeService.start(id));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<InternalJudgeResponse> finish(
            @PathVariable Long id,
            @RequestBody JudgeFinishRequest request
    ) {
        return ApiResponse.success(internalJudgeService.finish(id, request));
    }

    @PostMapping("/{id}/system-error")
    public ApiResponse<InternalJudgeResponse> systemError(
            @PathVariable Long id,
            @RequestBody JudgeSystemErrorRequest request
    ) {
        return ApiResponse.success(internalJudgeService.systemError(id, request));
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
