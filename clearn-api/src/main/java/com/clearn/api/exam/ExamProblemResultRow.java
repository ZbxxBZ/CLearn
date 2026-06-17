package com.clearn.api.exam;

public class ExamProblemResultRow {
    private Long userId;
    private String username;
    private Long problemId;
    private String title;
    private Integer maxScore;
    private Integer score;
    private Long bestSubmissionId;
    private String bestStatus;
    private Integer sortOrder;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getBestSubmissionId() {
        return bestSubmissionId;
    }

    public void setBestSubmissionId(Long bestSubmissionId) {
        this.bestSubmissionId = bestSubmissionId;
    }

    public String getBestStatus() {
        return bestStatus;
    }

    public void setBestStatus(String bestStatus) {
        this.bestStatus = bestStatus;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
