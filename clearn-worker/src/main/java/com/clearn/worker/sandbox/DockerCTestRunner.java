package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class DockerCTestRunner {
    private final ProcessExecutor processExecutor;
    private final Supplier<String> containerNameSupplier;

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
            long started = System.nanoTime();
            ProcessResult result = processExecutor.execute(buildCommand(workspace, containerName), workspace, input, timeoutMs);
            long elapsedMs = Math.max(1L, (System.nanoTime() - started) / 1_000_000L);
            if (result.timedOut()) {
                removeContainer(containerName, workspace);
            }
            return new RunResult(result.stdout(), result.stderr(), result.exitCode(), result.timedOut(), elapsedMs);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to run C program", ex);
        }
    }

    List<String> buildCommand(Path workspace, String containerName) {
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
        command.add("/work/main");
        return command;
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
