package com.clearn.worker.judge;

import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import com.clearn.common.judge.JudgeTaskMessage;
import com.clearn.worker.client.JudgeResultClient;
import com.clearn.worker.sandbox.DockerCTestRunner;
import com.clearn.worker.sandbox.GccCompiler;
import com.clearn.worker.sandbox.OutputComparator;
import com.clearn.worker.sandbox.WorkspaceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class JudgeCoordinator {
    private static final int ERROR_LIMIT = 4000;

    private final SubmissionLoadService submissionLoadService;
    private final GccCompiler compiler;
    private final DockerCTestRunner runner;
    private final OutputComparator outputComparator;
    private final JudgeResultClient judgeResultClient;
    private final WorkspaceFactory workspaceFactory;

    @Autowired
    public JudgeCoordinator(
            SubmissionLoadService submissionLoadService,
            GccCompiler compiler,
            DockerCTestRunner runner,
            OutputComparator outputComparator,
            JudgeResultClient judgeResultClient,
            WorkspaceFactory workspaceFactory
    ) {
        this.submissionLoadService = submissionLoadService;
        this.compiler = compiler;
        this.runner = runner;
        this.outputComparator = outputComparator;
        this.judgeResultClient = judgeResultClient;
        this.workspaceFactory = workspaceFactory;
    }

    public JudgeCoordinator(SubmissionLoadService submissionLoadService, JudgeResultClient judgeResultClient) {
        this(
                submissionLoadService,
                new GccCompiler(new com.clearn.worker.sandbox.ProcessExecutor()),
                new DockerCTestRunner(new com.clearn.worker.sandbox.ProcessExecutor()),
                new OutputComparator(),
                judgeResultClient,
                new WorkspaceFactory()
        );
    }

    public void judge(JudgeTaskMessage message) {
        Long submissionId = message.submissionId();
        judgeResultClient.start(submissionId);
        Path workspace = null;
        JudgeFinishRequest result;
        try {
            SubmissionLoadService.LoadedSubmission submission = submissionLoadService.load(submissionId);
            workspace = workspaceFactory.create();
            result = evaluate(workspace, submission);
        } catch (RuntimeException ex) {
            judgeResultClient.systemError(submissionId, new JudgeSystemErrorRequest(summary(ex.getMessage())));
            return;
        } finally {
            workspaceFactory.delete(workspace);
        }
        judgeResultClient.finish(submissionId, result);
    }

    private JudgeFinishRequest evaluate(Path workspace, SubmissionLoadService.LoadedSubmission submission) {
        GccCompiler.CompileResult compileResult = compiler.compile(workspace, submission.sourceCode());
        if (!compileResult.success()) {
            return result(SubmissionStatus.CE, 0, 0L, compileResult.stderr());
        }

        long totalTimeUsedMs = 0L;
        for (SubmissionLoadService.LoadedTestCase testCase : submission.testCases()) {
            DockerCTestRunner.RunResult runResult = runner.run(workspace, testCase.inputData(), submission.timeLimitMs());
            totalTimeUsedMs += runResult.timeUsedMs();

            if (runResult.timedOut()) {
                return result(SubmissionStatus.TLE, 0, totalTimeUsedMs, null);
            }
            if (runResult.exitCode() != 0) {
                return result(SubmissionStatus.RE, 0, totalTimeUsedMs, runResult.stderr());
            }
            if (!outputComparator.matches(testCase.expectedOutput(), runResult.stdout())) {
                return result(SubmissionStatus.WA, 0, totalTimeUsedMs, null);
            }
        }

        return result(SubmissionStatus.AC, 100, totalTimeUsedMs, null);
    }

    private JudgeFinishRequest result(SubmissionStatus status, Integer score, Long timeUsedMs, String errorMessage) {
        return new JudgeFinishRequest(
                status,
                score,
                timeUsedMs,
                null,
                summary(errorMessage)
        );
    }

    private String summary(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= ERROR_LIMIT) {
            return value;
        }
        return value.substring(0, ERROR_LIMIT);
    }
}
