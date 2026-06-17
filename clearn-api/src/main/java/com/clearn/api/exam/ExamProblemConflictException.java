package com.clearn.api.exam;

public class ExamProblemConflictException extends RuntimeException {
    public static final String MESSAGE = "Exam problem already exists for this exam";

    public ExamProblemConflictException() {
        super(MESSAGE);
    }

    public ExamProblemConflictException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
