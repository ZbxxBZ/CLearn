package com.clearn.api.problem;

public class TestCaseSortOrderConflictException extends RuntimeException {
    public static final String MESSAGE = "Test case sort order already exists for this problem";

    public TestCaseSortOrderConflictException() {
        super(MESSAGE);
    }

    public TestCaseSortOrderConflictException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
