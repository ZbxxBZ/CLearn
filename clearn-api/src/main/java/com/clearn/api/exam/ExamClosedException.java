package com.clearn.api.exam;

public class ExamClosedException extends RuntimeException {
    public ExamClosedException() {
        super("exam is not open");
    }
}
