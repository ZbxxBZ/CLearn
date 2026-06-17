package com.clearn.worker.sandbox;

public record ProcessResult(
        String stdout,
        String stderr,
        int exitCode,
        boolean timedOut
) {
}
