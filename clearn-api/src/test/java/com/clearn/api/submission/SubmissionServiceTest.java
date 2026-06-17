package com.clearn.api.submission;

import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.api.submission.dto.SubmissionCreateResponse;
import com.clearn.api.problem.ProblemMapper;
import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.clearn.common.judge.JudgeTaskMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class SubmissionServiceTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void createPracticeSubmissionPersistsPendingAndPublishesJudgeTask() {
        Long userId = userId("student");
        Long problemId = seedProblemId();
        SubmissionCreateRequest request = new SubmissionCreateRequest("""
                #include <stdio.h>
                int main(void) {
                    int a, b;
                    scanf("%d %d", &a, &b);
                    printf("%d\\n", a + b);
                    return 0;
                }
                """);

        SubmissionCreateResponse response = submissionService.createPracticeSubmission(userId, problemId, request);

        assertThat(response.submissionId()).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");
        Integer persisted = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from submissions
                        where id = ?
                          and user_id = ?
                          and problem_id = ?
                          and exam_id is null
                          and language = 'C'
                          and status = 'PENDING'
                          and score = 0
                          and passed_test_cases = 0
                          and total_test_cases = 0
                          and source_code like '%scanf%'
                        """,
                Integer.class,
                response.submissionId(),
                userId,
                problemId
        );
        assertThat(persisted).isEqualTo(1);

        ArgumentCaptor<JudgeTaskMessage> captor = ArgumentCaptor.forClass(JudgeTaskMessage.class);
        verify(judgeTaskPublisher).publish(captor.capture());
        JudgeTaskMessage message = captor.getValue();
        assertThat(message.submissionId()).isEqualTo(response.submissionId());
        assertThat(message.problemId()).isEqualTo(problemId);
        assertThat(message.language()).isEqualTo(Language.C);
        assertThat(message.mode()).isEqualTo(JudgeMode.PRACTICE);
        assertThat(message.examId()).isNull();
        assertThat(message.createdAt()).isNotNull();
    }

    @Test
    void createExamSubmissionPersistsExamIdAndPublishesExamJudgeTask() {
        Long userId = userId("student");
        Long problemId = seedProblemId();
        Long examId = insertOpenExamWithProblem(problemId, 100);

        SubmissionCreateResponse response = submissionService.createExamSubmission(
                userId,
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        );

        assertThat(response.submissionId()).isNotNull();
        assertThat(response.status()).isEqualTo("PENDING");
        Integer persisted = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from submissions
                        where id = ?
                          and user_id = ?
                          and problem_id = ?
                          and exam_id = ?
                          and status = 'PENDING'
                        """,
                Integer.class,
                response.submissionId(),
                userId,
                problemId,
                examId
        );
        assertThat(persisted).isEqualTo(1);

        ArgumentCaptor<JudgeTaskMessage> captor = ArgumentCaptor.forClass(JudgeTaskMessage.class);
        verify(judgeTaskPublisher).publish(captor.capture());
        JudgeTaskMessage message = captor.getValue();
        assertThat(message.mode()).isEqualTo(JudgeMode.EXAM);
        assertThat(message.examId()).isEqualTo(examId);
    }

    @Test
    void blankSourceCodeIsRejected() {
        assertThatThrownBy(() -> submissionService.createPracticeSubmission(
                userId("student"),
                seedProblemId(),
                new SubmissionCreateRequest("  ")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceCode");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void judgeTaskIsPublishedAfterSurroundingTransactionCommits() {
        Long userId = userId("student");
        Long problemId = seedProblemId();
        SubmissionCreateRequest request = new SubmissionCreateRequest("int main(void) { return 0; }");

        SubmissionCreateResponse response = transactionTemplate.execute(status -> {
            SubmissionCreateResponse created = submissionService.createPracticeSubmission(userId, problemId, request);

            assertThat(created.submissionId()).isNotNull();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from submissions where id = ? and status = 'PENDING'",
                    Integer.class,
                    created.submissionId()
            )).isEqualTo(1);
            verifyNoInteractions(judgeTaskPublisher);
            return created;
        });

        ArgumentCaptor<JudgeTaskMessage> captor = ArgumentCaptor.forClass(JudgeTaskMessage.class);
        verify(judgeTaskPublisher).publish(captor.capture());
        assertThat(captor.getValue().submissionId()).isEqualTo(response.submissionId());
        assertThat(captor.getValue().problemId()).isEqualTo(problemId);
    }

    @Test
    void multibyteSourceCodeOverByteLimitIsRejected() {
        SubmissionService limitedService = new SubmissionService(
                submissionMapper,
                problemMapper,
                judgeTaskPublisher,
                8
        );

        assertThatThrownBy(() -> limitedService.createPracticeSubmission(
                userId("student"),
                seedProblemId(),
                new SubmissionCreateRequest("你好abc")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceCode");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void missingProblemCannotBeSubmitted() {
        assertThatThrownBy(() -> submissionService.createPracticeSubmission(
                userId("student"),
                999999L,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("problem");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void disabledProblemCannotBeSubmitted() {
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
                        values (
                            'Disabled Submit Problem',
                            'Disabled problem.',
                            'Input.',
                            'Output.',
                            'EASY',
                            'disabled',
                            1000,
                            128,
                            100,
                            false
                        )
                        """
        );
        Long disabledProblemId = jdbcTemplate.queryForObject(
                "select id from problems where title = 'Disabled Submit Problem'",
                Long.class
        );

        assertThatThrownBy(() -> submissionService.createPracticeSubmission(
                userId("student"),
                disabledProblemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("problem");

        verifyNoInteractions(judgeTaskPublisher);
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "select id from users where username = ?",
                Long.class,
                username
        );
    }

    private Long seedProblemId() {
        return jdbcTemplate.queryForObject(
                "select id from problems where title = 'A+B Problem'",
                Long.class
        );
    }

    private Long insertOpenExamWithProblem(Long problemId, int score) {
        jdbcTemplate.update(
                """
                        insert into exams (
                            title,
                            description,
                            start_time,
                            end_time,
                            enabled
                        )
                        values (
                            'Submission Service Exam',
                            'Open exam for submission service tests.',
                            DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
                            true
                        )
                        """
        );
        Long examId = jdbcTemplate.queryForObject(
                "select id from exams where title = 'Submission Service Exam'",
                Long.class
        );
        jdbcTemplate.update(
                """
                        insert into exam_problems (
                            exam_id,
                            problem_id,
                            score,
                            sort_order
                        )
                        values (?, ?, ?, 1)
                        """,
                examId,
                problemId,
                score
        );
        return examId;
    }

}
