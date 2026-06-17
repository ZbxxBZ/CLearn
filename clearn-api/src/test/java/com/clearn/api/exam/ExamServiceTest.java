package com.clearn.api.exam;

import com.clearn.api.submission.JudgeTaskPublisher;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import com.clearn.api.exam.dto.ExamResultResponse;
import com.clearn.common.enums.JudgeMode;
import com.clearn.common.judge.JudgeTaskMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class ExamServiceTest {
    @Autowired
    private ExamService examService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void rejectsSubmissionBeforeExamStarts() {
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Future Exam",
                problemId,
                nowUtc().plusHours(1),
                nowUtc().plusHours(2),
                true,
                100
        );

        assertThatThrownBy(() -> examService.createExamSubmission(
                userId("student"),
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(ExamClosedException.class)
                .hasMessageContaining("exam is not open");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void rejectsSubmissionAfterExamEnds() {
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Past Exam",
                problemId,
                nowUtc().minusHours(2),
                nowUtc().minusHours(1),
                true,
                100
        );

        assertThatThrownBy(() -> examService.createExamSubmission(
                userId("student"),
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(ExamClosedException.class)
                .hasMessageContaining("exam is not open");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void rejectsSubmissionWhenProblemIsNotInExam() {
        Long problemId = seedProblemId();
        Long otherProblemId = insertProblem("Exam Other Problem");
        Long examId = insertExamWithProblem(
                "Open Exam",
                problemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                100
        );

        assertThatThrownBy(() -> examService.createExamSubmission(
                userId("student"),
                examId,
                otherProblemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("exam problem");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void createsSubmissionForOpenExamProblem() {
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Current Exam",
                problemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                100
        );

        SubmissionCreateResponse response = examService.createExamSubmission(
                userId("student"),
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        );

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "select exam_id from submissions where id = ?",
                Long.class,
                response.submissionId()
        )).isEqualTo(examId);

        ArgumentCaptor<JudgeTaskMessage> captor = ArgumentCaptor.forClass(JudgeTaskMessage.class);
        verify(judgeTaskPublisher).publish(captor.capture());
        assertThat(captor.getValue().mode()).isEqualTo(JudgeMode.EXAM);
        assertThat(captor.getValue().examId()).isEqualTo(examId);
    }

    @Test
    void myResultUsesBestScorePerProblemAndZeroForUnsubmittedProblems() {
        Long userId = userId("student");
        Long firstProblemId = seedProblemId();
        Long secondProblemId = insertProblem("Second Exam Problem");
        Long thirdProblemId = insertProblem("Third Exam Problem");
        Long examId = insertExamWithProblem(
                "Result Exam",
                firstProblemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                100
        );
        addExamProblem(examId, secondProblemId, 50, 2);
        addExamProblem(examId, thirdProblemId, 30, 3);
        insertJudgedSubmission(userId, examId, firstProblemId, "WA", 0);
        Long bestSubmissionId = insertJudgedSubmission(userId, examId, firstProblemId, "AC", 100);
        insertJudgedSubmission(userId, examId, secondProblemId, "WA", 0);

        ExamResultResponse result = examService.getMyResult(userId, examId);

        assertThat(result.examId()).isEqualTo(examId);
        assertThat(result.totalScore()).isEqualTo(100);
        assertThat(result.maxScore()).isEqualTo(180);
        assertThat(result.problems()).hasSize(3);
        assertThat(result.problems().get(0).score()).isEqualTo(100);
        assertThat(result.problems().get(0).bestSubmissionId()).isEqualTo(bestSubmissionId);
        assertThat(result.problems().get(1).score()).isZero();
        assertThat(result.problems().get(2).score()).isZero();
        assertThat(result.problems().get(2).bestSubmissionId()).isNull();
    }

    @Test
    void myResultUsesExamProblemScoreInsteadOfRawSubmissionScore() {
        Long userId = userId("student");
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Scaled Result Exam",
                problemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                50
        );
        Long bestSubmissionId = insertJudgedSubmission(userId, examId, problemId, "AC", 100);

        ExamResultResponse result = examService.getMyResult(userId, examId);

        assertThat(result.totalScore()).isEqualTo(50);
        assertThat(result.maxScore()).isEqualTo(50);
        assertThat(result.problems().get(0).score()).isEqualTo(50);
        assertThat(result.problems().get(0).bestSubmissionId()).isEqualTo(bestSubmissionId);
    }

    @Test
    void myResultIgnoresDisabledExamProblems() {
        Long userId = userId("student");
        Long enabledProblemId = seedProblemId();
        Long disabledProblemId = insertProblem("Disabled Result Problem", false);
        Long examId = insertExamWithProblem(
                "Disabled My Result Exam",
                enabledProblemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                40
        );
        addExamProblem(examId, disabledProblemId, 60, 2);
        insertJudgedSubmission(userId, examId, enabledProblemId, "AC", 100);
        insertJudgedSubmission(userId, examId, disabledProblemId, "AC", 100);

        ExamResultResponse result = examService.getMyResult(userId, examId);

        assertThat(result.totalScore()).isEqualTo(40);
        assertThat(result.maxScore()).isEqualTo(40);
        assertThat(result.problems())
                .extracting(com.clearn.api.exam.dto.ExamProblemResultResponse::problemId)
                .containsExactly(enabledProblemId);
    }

    private Long insertExamWithProblem(
            String title,
            Long problemId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean enabled,
            int score
    ) {
        jdbcTemplate.update(
                """
                        insert into exams (title, description, start_time, end_time, enabled)
                        values (?, 'Exam.', ?, ?, ?)
                        """,
                title,
                startTime,
                endTime,
                enabled
        );
        Long examId = jdbcTemplate.queryForObject(
                "select id from exams where title = ?",
                Long.class,
                title
        );
        addExamProblem(examId, problemId, score, 1);
        return examId;
    }

    private void addExamProblem(Long examId, Long problemId, int score, int sortOrder) {
        jdbcTemplate.update(
                "insert into exam_problems (exam_id, problem_id, score, sort_order) values (?, ?, ?, ?)",
                examId,
                problemId,
                score,
                sortOrder
        );
    }

    private Long insertProblem(String title) {
        return insertProblem(title, true);
    }

    private Long insertProblem(String title, boolean enabled) {
        jdbcTemplate.update(
                """
                        insert into problems (
                            title,
                            description,
                            input_description,
                            output_description,
                            difficulty,
                            tags,
                            time_limit_ms,
                            memory_limit_mb,
                            score,
                            enabled
                        )
                        values (?, 'Problem.', 'Input.', 'Output.', 'EASY', 'exam', 1000, 128, 100, ?)
                        """,
                title,
                enabled
        );
        return jdbcTemplate.queryForObject(
                "select id from problems where title = ?",
                Long.class,
                title
        );
    }

    private Long insertJudgedSubmission(Long userId, Long examId, Long problemId, String status, int score) {
        String sourceCode = "int main(void) { return " + System.nanoTime() + "; }";
        jdbcTemplate.update(
                """
                        insert into submissions (
                            user_id,
                            problem_id,
                            exam_id,
                            language,
                            source_code,
                            status,
                            score,
                            passed_test_cases,
                            total_test_cases,
                            judged_at
                        )
                        values (?, ?, ?, 'C', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                userId,
                problemId,
                examId,
                sourceCode,
                status,
                score,
                score > 0 ? 5 : 0,
                5
        );
        return jdbcTemplate.queryForObject(
                "select id from submissions where source_code = ?",
                Long.class,
                sourceCode
        );
    }

    private Long seedProblemId() {
        return jdbcTemplate.queryForObject(
                "select id from problems where title = 'A+B Problem'",
                Long.class
        );
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "select id from users where username = ?",
                Long.class,
                username
        );
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
