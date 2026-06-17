package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessExecutor {

    public ProcessResult execute(List<String> command, Path directory, String input, long timeoutMs) throws IOException {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .start();

        CompletableFuture<String> stdout = readAsync(process.getInputStream());
        CompletableFuture<String> stderr = readAsync(process.getErrorStream());
        CompletableFuture<Void> stdin = writeInputAsync(process, input);

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
            }
            stdin.join();
            return new ProcessResult(
                    stdout.join(),
                    stderr.join(),
                    finished ? process.exitValue() : 124,
                    !finished
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IllegalStateException("process interrupted", ex);
        }
    }

    private CompletableFuture<String> readAsync(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalStateException("failed to read process stream", ex);
            }
        });
    }

    private CompletableFuture<Void> writeInputAsync(Process process, String input) {
        return CompletableFuture.runAsync(() -> {
            try (java.io.OutputStream stream = process.getOutputStream()) {
                if (input != null && !input.isEmpty()) {
                    stream.write(input.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException ex) {
                if (process.isAlive()) {
                    throw new IllegalStateException("failed to write process input", ex);
                }
            }
        });
    }
}
