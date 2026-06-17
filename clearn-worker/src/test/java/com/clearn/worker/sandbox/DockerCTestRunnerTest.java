package com.clearn.worker.sandbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerCTestRunnerTest {

    @Test
    void buildsDockerCommandWithSandboxLimitsAndNamedContainer() {
        RecordingProcessExecutor executor = new RecordingProcessExecutor(new ProcessResult("", "", 0, false));
        DockerCTestRunner runner = new DockerCTestRunner(executor, () -> "clearn-judge-test");

        runner.run(Path.of("workspace"), "1 2\n", 1000);

        List<String> command = executor.commands().get(0);
        assertThat(command).containsExactly(
                "docker",
                "run",
                "--rm",
                "-i",
                "--name",
                "clearn-judge-test",
                "--network",
                "none",
                "--cpus",
                "1",
                "--memory",
                "128m",
                "--pids-limit",
                "64",
                "--read-only",
                "--tmpfs",
                "/tmp:rw,noexec,nosuid,size=16m",
                "-v",
                Path.of("workspace").toAbsolutePath() + ":/work:ro",
                "-w",
                "/work",
                "clearn-c-runner:latest",
                "/work/main"
        );
        assertThat(executor.inputs()).containsExactly("1 2\n");
    }

    @Test
    void removesContainerWhenDockerRunTimesOut() {
        RecordingProcessExecutor executor = new RecordingProcessExecutor(
                new ProcessResult("", "", 124, true),
                new ProcessResult("", "", 0, false)
        );
        DockerCTestRunner runner = new DockerCTestRunner(executor, () -> "clearn-judge-timeout");

        DockerCTestRunner.RunResult result = runner.run(Path.of("workspace"), "", 1000);

        assertThat(result.timedOut()).isTrue();
        assertThat(executor.commands()).hasSize(2);
        assertThat(executor.commands().get(1)).containsExactly("docker", "rm", "-f", "clearn-judge-timeout");
    }

    private static final class RecordingProcessExecutor extends ProcessExecutor {
        private final List<ProcessResult> results;
        private final List<List<String>> commands = new ArrayList<>();
        private final List<String> inputs = new ArrayList<>();
        private int index;

        RecordingProcessExecutor(ProcessResult... results) {
            this.results = List.of(results);
        }

        @Override
        public ProcessResult execute(List<String> command, Path directory, String input, long timeoutMs) throws IOException {
            commands.add(List.copyOf(command));
            inputs.add(input);
            if (index >= results.size()) {
                return new ProcessResult("", "", 0, false);
            }
            return results.get(index++);
        }

        List<List<String>> commands() {
            return commands;
        }

        List<String> inputs() {
            return inputs;
        }
    }
}
