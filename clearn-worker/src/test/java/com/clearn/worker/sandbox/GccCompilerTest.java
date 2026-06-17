package com.clearn.worker.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GccCompilerTest {

    @TempDir
    Path workspace;

    @Test
    void linksMathLibraryForMathHeaderFunctions() throws IOException {
        RecordingProcessExecutor executor = new RecordingProcessExecutor(new ProcessResult("", "", 0, false));
        GccCompiler compiler = new GccCompiler(executor);

        compiler.compile(workspace, """
                #include <math.h>
                int main(void) {
                    return (int)sqrt(4.0);
                }
                """);

        assertThat(executor.commands()).singleElement()
                .satisfies(command -> assertThat(command).containsSubsequence("main.c", "-lm"));
    }

    private static final class RecordingProcessExecutor extends ProcessExecutor {
        private final ProcessResult result;
        private final List<List<String>> commands = new ArrayList<>();

        RecordingProcessExecutor(ProcessResult result) {
            this.result = result;
        }

        @Override
        public ProcessResult execute(List<String> command, Path directory, String input, long timeoutMs) throws IOException {
            commands.add(List.copyOf(command));
            Files.writeString(directory.resolve("main"), "");
            return result;
        }

        List<List<String>> commands() {
            return commands;
        }
    }
}
