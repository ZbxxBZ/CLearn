package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

@Component
public class GccCompiler {
    private static final int ERROR_LIMIT = 4000;
    private final ProcessExecutor processExecutor;

    @Autowired
    public GccCompiler(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public CompileResult compile(Path workspace, String sourceCode) {
        try {
            Files.writeString(workspace.resolve("main.c"), sourceCode, StandardCharsets.UTF_8);
            ProcessResult result = processExecutor.execute(
                    List.of("gcc", "-O2", "-std=c11", "main.c", "-lm", "-o", "main"),
                    workspace,
                    "",
                    10_000
            );
            if (result.exitCode() == 0) {
                ensureExecutable(workspace.resolve("main"));
            }
            return new CompileResult(result.exitCode() == 0, truncate(result.stderr()), result.exitCode());
        } catch (IOException ex) {
            throw new IllegalStateException("failed to compile C source", ex);
        }
    }

    private void ensureExecutable(Path executable) throws IOException {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private String truncate(String value) {
        if (value == null || value.length() <= ERROR_LIMIT) {
            return value;
        }
        return value.substring(0, ERROR_LIMIT);
    }

    public record CompileResult(
            boolean success,
            String stderr,
            int exitCode
    ) {
    }
}
