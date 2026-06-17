package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DockerCTestRunner {
    private final ProcessExecutor processExecutor;
    private final Supplier<String> containerNameSupplier;
    private static final String TIME_MARKER_PREFIX = "CLEARN_TIME_NS=";
    private static final Pattern TIME_MARKER_PATTERN = Pattern.compile("(?m)^CLEARN_TIME_NS=([^\\r\\n]+)$");
    private static final Pattern TIME_MARKER_LINE_PATTERN = Pattern.compile("(?m)\\R?^CLEARN_TIME_NS=[^\\r\\n]+\\R?");

    @Autowired
    public DockerCTestRunner(ProcessExecutor processExecutor) {
        this(processExecutor, () -> "clearn-judge-" + UUID.randomUUID());
    }

    DockerCTestRunner(ProcessExecutor processExecutor, Supplier<String> containerNameSupplier) {
        this.processExecutor = processExecutor;
        this.containerNameSupplier = containerNameSupplier;
    }

    public RunResult run(Path workspace, String input, int timeoutMs) {
        String containerName = containerNameSupplier.get();
        try {
            ProcessResult result = processExecutor.execute(
                    buildCommand(workspace, containerName, timeoutMs),
                    workspace,
                    input,
                    dockerTimeoutMs(timeoutMs)
            );
            long elapsedMs = parseTimeUsedMs(result.stderr());
            if (result.timedOut()) {
                removeContainer(containerName, workspace);
            }
            boolean timedOut = result.timedOut() || result.exitCode() == 124 || result.exitCode() == 137;
            return new RunResult(result.stdout(), stripTimeMarker(result.stderr()), result.exitCode(), timedOut, elapsedMs);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to run C program", ex);
        }
    }

    List<String> buildCommand(Path workspace, String containerName, int timeoutMs) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("-i");
        command.add("--name");
        command.add(containerName);
        command.add("--network");
        command.add("none");
        command.add("--cpus");
        command.add("1");
        command.add("--memory");
        command.add("128m");
        command.add("--pids-limit");
        command.add("64");
        command.add("--read-only");
        command.add("--tmpfs");
        command.add("/tmp:rw,noexec,nosuid,size=16m");
        command.add("-v");
        command.add(workspace.toAbsolutePath() + ":/work:ro");
        command.add("-w");
        command.add("/work");
        command.add("clearn-c-runner:latest");
        command.add("sh");
        command.add("-c");
        command.add(timeCommand(timeoutMs));
        return command;
    }

    private String timeCommand(int timeoutMs) {
        String limit = timeoutSeconds(timeoutMs);
        return "start_ns=$(date +%s%N); "
                + "timeout -s TERM -k 1s " + limit + " /work/main; "
                + "code=$?; "
                + "end_ns=$(date +%s%N); "
                + "elapsed_ns=$((end_ns - start_ns)); "
                + "printf '\\n" + TIME_MARKER_PREFIX + "%s\\n' \"$elapsed_ns\" >&2; "
                + "exit \"$code\"";
    }

    private String timeoutSeconds(int timeoutMs) {
        long seconds = Math.max(1L, (timeoutMs + 999L) / 1000L);
        return seconds + "s";
    }

    private long dockerTimeoutMs(int timeoutMs) {
        return Math.max(1L, timeoutMs) + 5_000L;
    }

    private long parseTimeUsedMs(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return 1L;
        }
        Matcher matcher = TIME_MARKER_PATTERN.matcher(stderr);
        if (!matcher.find()) {
            return 1L;
        }
        try {
            long nanos = Long.parseLong(matcher.group(1).trim());
            return Math.max(1L, Math.round(nanos / 1_000_000.0d));
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    private String stripTimeMarker(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return stderr;
        }
        return TIME_MARKER_LINE_PATTERN.matcher(stderr).replaceFirst("");
    }

    private void removeContainer(String containerName, Path workspace) {
        try {
            processExecutor.execute(List.of("docker", "rm", "-f", containerName), workspace, "", 5_000);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to remove timed out C runner container", ex);
        }
    }

    public record RunResult(
            String stdout,
            String stderr,
            int exitCode,
            boolean timedOut,
            long timeUsedMs
    ) {
    }
}
