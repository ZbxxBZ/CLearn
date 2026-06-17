package com.clearn.worker.judge;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.judge.JudgeTaskMessage;
import com.clearn.worker.client.JudgeResultClient;
import com.clearn.worker.sandbox.DockerCTestRunner;
import com.clearn.worker.sandbox.GccCompiler;
import com.clearn.worker.sandbox.OutputComparator;
import com.clearn.worker.sandbox.WorkspaceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JudgeCoordinatorTest {

    @Test
    void returnsAcceptedWhenAllCasesMatch() {
        SubmissionLoadService loader = fakeLoaderWithCases(
                testCase("1 2\n", "3\n"),
                testCase("2 5\n", "7\n")
        );
        GccCompiler compiler = fakeCompilerSuccess();
        DockerCTestRunner runner = mock(DockerCTestRunner.class);
        when(runner.run(Path.of("workspace"), "1 2\n", 1000)).thenReturn(successfulRun("3\n", 11));
        when(runner.run(Path.of("workspace"), "2 5\n", 1000)).thenReturn(successfulRun("7\n\n", 17));
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, compiler, runner, client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result ->
                result.status() == SubmissionStatus.AC
                        && result.score() == 100
                        && result.timeUsedMs() == 28
                        && result.errorMessage() == null
        ));
    }

    @Test
    void returnsWrongAnswerOnFirstMismatchedCase() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("1 2\n", "3\n"));
        GccCompiler compiler = fakeCompilerSuccess();
        DockerCTestRunner runner = fakeRunnerOutput("4\n");
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, compiler, runner, client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result ->
                result.status() == SubmissionStatus.WA
                        && result.score() == 0
        ));
    }

    @Test
    void returnsCompileErrorWhenCompilationFails() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("", ""));
        GccCompiler compiler = mock(GccCompiler.class);
        when(compiler.compile(Path.of("workspace"), "bad source")).thenReturn(new GccCompiler.CompileResult(
                false,
                "main.c:1: error: expected ';'",
                1
        ));
        DockerCTestRunner runner = mock(DockerCTestRunner.class);
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, compiler, runner, client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result ->
                result.status() == SubmissionStatus.CE
                        && result.score() == 0
                        && result.errorMessage().contains("expected")
        ));
        verify(runner, never()).run(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void returnsTimeLimitExceededWhenRunnerTimesOut() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("", ""));
        GccCompiler compiler = fakeCompilerSuccess();
        DockerCTestRunner runner = mock(DockerCTestRunner.class);
        when(runner.run(Path.of("workspace"), "", 1000)).thenReturn(new DockerCTestRunner.RunResult(
                "",
                "",
                124,
                true,
                1000
        ));
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, compiler, runner, client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result ->
                result.status() == SubmissionStatus.TLE
                        && result.score() == 0
        ));
    }

    @Test
    void returnsRuntimeErrorWhenRunnerExitsNonZero() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("", ""));
        GccCompiler compiler = fakeCompilerSuccess();
        DockerCTestRunner runner = mock(DockerCTestRunner.class);
        when(runner.run(Path.of("workspace"), "", 1000)).thenReturn(new DockerCTestRunner.RunResult(
                "",
                "segmentation fault",
                139,
                false,
                3
        ));
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, compiler, runner, client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result ->
                result.status() == SubmissionStatus.RE
                        && result.score() == 0
                        && result.errorMessage().contains("segmentation fault")
        ));
    }

    @Test
    void reportsSystemErrorOnUnexpectedException() {
        SubmissionLoadService loader = mock(SubmissionLoadService.class);
        when(loader.load(1L)).thenThrow(new IllegalStateException("database unavailable"));
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, fakeCompilerSuccess(), mock(DockerCTestRunner.class), client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).systemError(eq(1L), argThat(result -> result.errorMessage().contains("database unavailable")));
        verify(client, never()).finish(eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void propagatesFinishFailureWithoutReportingSystemError() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("1 2\n", "3\n"));
        DockerCTestRunner runner = fakeRunnerOutput("3\n");
        JudgeResultClient client = mock(JudgeResultClient.class);
        RuntimeException finishFailure = new IllegalStateException("internal api unavailable");
        org.mockito.Mockito.doThrow(finishFailure).when(client)
                .finish(eq(1L), org.mockito.ArgumentMatchers.any());

        JudgeCoordinator coordinator = coordinator(loader, fakeCompilerSuccess(), runner, client);

        assertThatThrownBy(() -> coordinator.judge(messageForSubmission(1L)))
                .isSameAs(finishFailure);
        verify(client, never()).systemError(eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletesWorkspaceAfterJudging() {
        SubmissionLoadService loader = fakeLoaderWithCases(testCase("1 2\n", "3\n"));
        DockerCTestRunner runner = fakeRunnerOutput("3\n");
        JudgeResultClient client = mock(JudgeResultClient.class);
        TrackingWorkspaceFactory workspaceFactory = new TrackingWorkspaceFactory();
        JudgeCoordinator coordinator = new JudgeCoordinator(
                loader,
                fakeCompilerSuccess(),
                runner,
                new OutputComparator(),
                client,
                workspaceFactory
        );

        coordinator.judge(messageForSubmission(1L));

        assertThat(workspaceFactory.deletedPath()).isEqualTo(Path.of("workspace"));
    }

    @Test
    void startsSubmissionBeforeLoadingAndJudging() {
        SubmissionLoadService loader = fakeLoaderWithCases();
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = coordinator(loader, fakeCompilerSuccess(), mock(DockerCTestRunner.class), client);
        coordinator.judge(messageForSubmission(1L));

        InOrder inOrder = inOrder(client, loader);
        inOrder.verify(client).start(1L);
        inOrder.verify(loader).load(1L);
    }

    private JudgeCoordinator coordinator(
            SubmissionLoadService loader,
            GccCompiler compiler,
            DockerCTestRunner runner,
            JudgeResultClient client
    ) {
        return new JudgeCoordinator(loader, compiler, runner, new OutputComparator(), client, new WorkspaceFactory() {
            @Override
            public Path create() {
                return Path.of("workspace");
            }
        });
    }

    private SubmissionLoadService fakeLoaderWithCases(SubmissionLoadService.LoadedTestCase... testCases) {
        SubmissionLoadService loader = mock(SubmissionLoadService.class);
        when(loader.load(1L)).thenReturn(new SubmissionLoadService.LoadedSubmission(
                1L,
                10L,
                Language.C,
                "bad source",
                1000,
                128,
                List.of(testCases)
        ));
        return loader;
    }

    private GccCompiler fakeCompilerSuccess() {
        GccCompiler compiler = mock(GccCompiler.class);
        when(compiler.compile(Path.of("workspace"), "bad source")).thenReturn(new GccCompiler.CompileResult(true, "", 0));
        return compiler;
    }

    private DockerCTestRunner fakeRunnerOutput(String stdout) {
        DockerCTestRunner runner = mock(DockerCTestRunner.class);
        when(runner.run(Path.of("workspace"), "1 2\n", 1000)).thenReturn(successfulRun(stdout, 5));
        return runner;
    }

    private DockerCTestRunner.RunResult successfulRun(String stdout, long timeUsedMs) {
        return new DockerCTestRunner.RunResult(stdout, "", 0, false, timeUsedMs);
    }

    private SubmissionLoadService.LoadedTestCase testCase(String input, String expectedOutput) {
        return new SubmissionLoadService.LoadedTestCase(1L, 10L, input, expectedOutput, false, 1);
    }

    private JudgeTaskMessage messageForSubmission(Long submissionId) {
        return new JudgeTaskMessage(
                submissionId,
                10L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );
    }

    private static final class TrackingWorkspaceFactory extends WorkspaceFactory {
        private Path deletedPath;

        @Override
        public Path create() {
            return Path.of("workspace");
        }

        @Override
        public void delete(Path workspace) {
            this.deletedPath = workspace;
        }

        Path deletedPath() {
            return deletedPath;
        }
    }
}
