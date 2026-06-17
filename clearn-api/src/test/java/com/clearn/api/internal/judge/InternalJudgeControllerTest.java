package com.clearn.api.internal.judge;

import com.clearn.common.enums.SubmissionStatus;
import com.clearn.common.judge.JudgeFinishRequest;
import com.clearn.common.judge.JudgeSystemErrorRequest;
import com.clearn.common.json.JsonMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalJudgeControllerTest {
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMappers.objectMapper();

    @Test
    void rejectsMissingInternalToken() throws Exception {
        mockMvc.perform(post("/api/internal/judge/submissions/1/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startChangesPendingSubmissionToJudging() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.PENDING);

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/start", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionId").value(submissionId))
                .andExpect(jsonPath("$.data.status").value("JUDGING"));

        assertThat(statusOf(submissionId)).isEqualTo("JUDGING");
        assertThat(judgedAtOf(submissionId)).isNull();
    }

    @Test
    void startDoesNotOverwriteTerminalSubmission() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.AC, 100, 10, 256, null, LocalDateTime.now());

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/start", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AC"));

        assertThat(statusOf(submissionId)).isEqualTo("AC");
        assertThat(scoreOf(submissionId)).isEqualTo(100);
    }

    @Test
    void finishChangesJudgingSubmissionToTerminalStatus() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.JUDGING);
        JudgeFinishRequest request = new JudgeFinishRequest(
                SubmissionStatus.AC,
                100,
                5,
                5,
                12L,
                2048L,
                "all cases passed"
        );

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/finish", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AC"));

        SubmissionRow row = rowOf(submissionId);
        assertThat(row.status()).isEqualTo("AC");
        assertThat(row.score()).isEqualTo(100);
        assertThat(row.passedTestCases()).isEqualTo(5);
        assertThat(row.totalTestCases()).isEqualTo(5);
        assertThat(row.timeUsedMs()).isEqualTo(12);
        assertThat(row.memoryUsedKb()).isEqualTo(2048);
        assertThat(row.errorMessage()).isEqualTo("all cases passed");
        assertThat(row.judgedAt()).isNotNull();
    }

    @Test
    void finishRejectsNonTerminalStatus() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.JUDGING);
        JudgeFinishRequest request = new JudgeFinishRequest(
                SubmissionStatus.PENDING,
                0,
                0,
                5,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/finish", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(statusOf(submissionId)).isEqualTo("JUDGING");
    }

    @Test
    void finishRejectsNegativeMetrics() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.JUDGING);
        JudgeFinishRequest request = new JudgeFinishRequest(
                SubmissionStatus.WA,
                -1,
                -1,
                -1,
                -12L,
                -2048L,
                "invalid metrics"
        );

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/finish", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        SubmissionRow row = rowOf(submissionId);
        assertThat(row.status()).isEqualTo("JUDGING");
        assertThat(row.score()).isEqualTo(0);
        assertThat(row.timeUsedMs()).isNull();
        assertThat(row.memoryUsedKb()).isNull();
    }

    @Test
    void finishDoesNotOverwriteNonJudgingSubmission() throws Exception {
        LocalDateTime judgedAt = LocalDateTime.now().minusMinutes(1);
        Long submissionId = insertSubmission(SubmissionStatus.WA, 0, 33, 4096, "wrong answer", judgedAt);
        JudgeFinishRequest request = new JudgeFinishRequest(
                SubmissionStatus.AC,
                100,
                5,
                5,
                12L,
                2048L,
                null
        );

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/finish", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WA"));

        SubmissionRow row = rowOf(submissionId);
        assertThat(row.status()).isEqualTo("WA");
        assertThat(row.score()).isEqualTo(0);
        assertThat(row.timeUsedMs()).isEqualTo(33);
        assertThat(row.memoryUsedKb()).isEqualTo(4096);
        assertThat(row.errorMessage()).isEqualTo("wrong answer");
    }

    @Test
    void systemErrorMarksPendingSubmissionAsSystemError() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.PENDING);
        JudgeSystemErrorRequest request = new JudgeSystemErrorRequest("worker failed");

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/system-error", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SE"));

        SubmissionRow row = rowOf(submissionId);
        assertThat(row.status()).isEqualTo("SE");
        assertThat(row.errorMessage()).isEqualTo("worker failed");
        assertThat(row.judgedAt()).isNotNull();
    }

    @Test
    void systemErrorDoesNotOverwriteTerminalSubmission() throws Exception {
        Long submissionId = insertSubmission(SubmissionStatus.CE, 0, null, null, "compile error", LocalDateTime.now());
        JudgeSystemErrorRequest request = new JudgeSystemErrorRequest("worker failed");

        mockMvc.perform(post("/api/internal/judge/submissions/{id}/system-error", submissionId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CE"));

        SubmissionRow row = rowOf(submissionId);
        assertThat(row.status()).isEqualTo("CE");
        assertThat(row.errorMessage()).isEqualTo("compile error");
    }

    @Test
    void missingSubmissionReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/internal/judge/submissions/999999/start")
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isNotFound());
    }

    private Long insertSubmission(SubmissionStatus status) {
        return insertSubmission(status, 0, null, null, null, null);
    }

    private Long insertSubmission(
            SubmissionStatus status,
            Integer score,
            Integer timeUsedMs,
            Integer memoryUsedKb,
            String errorMessage,
            LocalDateTime judgedAt
    ) {
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
                            time_used_ms,
                            memory_used_kb,
                            error_message,
                            judged_at
                        )
                        values (?, ?, null, 'C', ?, ?, ?, 0, 0, ?, ?, ?, ?)
                        """,
                userId("student"),
                seedProblemId(),
                sourceCode,
                status.name(),
                score,
                timeUsedMs,
                memoryUsedKb,
                errorMessage,
                judgedAt
        );
        return jdbcTemplate.queryForObject(
                "select id from submissions where source_code = ?",
                Long.class,
                sourceCode
        );
    }

    private String statusOf(Long submissionId) {
        return jdbcTemplate.queryForObject(
                "select status from submissions where id = ?",
                String.class,
                submissionId
        );
    }

    private Integer scoreOf(Long submissionId) {
        return jdbcTemplate.queryForObject(
                "select score from submissions where id = ?",
                Integer.class,
                submissionId
        );
    }

    private LocalDateTime judgedAtOf(Long submissionId) {
        return jdbcTemplate.queryForObject(
                "select judged_at from submissions where id = ?",
                LocalDateTime.class,
                submissionId
        );
    }

    private SubmissionRow rowOf(Long submissionId) {
        return jdbcTemplate.queryForObject(
                """
                        select status,
                               score,
                               passed_test_cases,
                               total_test_cases,
                               time_used_ms,
                               memory_used_kb,
                               error_message,
                               judged_at
                        from submissions
                        where id = ?
                        """,
                (rs, rowNum) -> new SubmissionRow(
                        rs.getString("status"),
                        rs.getInt("score"),
                        (Integer) rs.getObject("passed_test_cases"),
                        (Integer) rs.getObject("total_test_cases"),
                        (Integer) rs.getObject("time_used_ms"),
                        (Integer) rs.getObject("memory_used_kb"),
                        rs.getString("error_message"),
                        rs.getObject("judged_at", LocalDateTime.class)
                ),
                submissionId
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

    private record SubmissionRow(
            String status,
            Integer score,
            Integer passedTestCases,
            Integer totalTestCases,
            Integer timeUsedMs,
            Integer memoryUsedKb,
            String errorMessage,
            LocalDateTime judgedAt
    ) {
    }
}
