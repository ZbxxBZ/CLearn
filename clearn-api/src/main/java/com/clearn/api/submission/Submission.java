package com.clearn.api.submission;

import java.time.LocalDateTime;

public class Submission {
    private Long id;
    private Long userId;
    private Long problemId;
    private Long examId;
    private String language;
    private String sourceCode;
    private String status;
    private Integer score;
    private Integer passedTestCases;
    private Integer totalTestCases;
    private Integer timeUsedMs;
    private Integer memoryUsedKb;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime judgedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getPassedTestCases() {
        return passedTestCases;
    }

    public void setPassedTestCases(Integer passedTestCases) {
        this.passedTestCases = passedTestCases;
    }

    public Integer getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(Integer totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public Integer getTimeUsedMs() {
        return timeUsedMs;
    }

    public void setTimeUsedMs(Integer timeUsedMs) {
        this.timeUsedMs = timeUsedMs;
    }

    public Integer getMemoryUsedKb() {
        return memoryUsedKb;
    }

    public void setMemoryUsedKb(Integer memoryUsedKb) {
        this.memoryUsedKb = memoryUsedKb;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(LocalDateTime judgedAt) {
        this.judgedAt = judgedAt;
    }
}
