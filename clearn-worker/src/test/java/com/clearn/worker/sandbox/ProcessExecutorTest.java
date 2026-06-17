package com.clearn.worker.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessExecutorTest {

    @Test
    void drainsStdoutAndStderrWhileProcessIsRunning() throws Exception {
        ProcessExecutor executor = new ProcessExecutor();
        String javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                isWindows() ? "java.exe" : "java"
        ).toString();

        ProcessResult result = executor.execute(
                List.of(
                        javaExecutable,
                        "-cp",
                        System.getProperty("java.class.path"),
                        ProcessFlooder.class.getName()
                ),
                Path.of("."),
                "",
                5_000
        );

        assertThat(result.timedOut()).isFalse();
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).hasSizeGreaterThan(1_000_000);
        assertThat(result.stderr()).hasSizeGreaterThan(1_000_000);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
