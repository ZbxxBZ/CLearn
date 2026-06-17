package com.clearn.api.exam;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminExamControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminCreatesUpdatesListsAndDisablesExam() throws Exception {
        String token = loginAndReadToken("admin");
        String title = uniqueTitle("Admin Created Exam");
        Long examId = createExam(token, title, true);

        mockMvc.perform(get("/api/admin/exams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].title", hasItem(title)));

        mockMvc.perform(get("/api/admin/exams/{id}", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(examId))
                .andExpect(jsonPath("$.data.enabled").value(true));

        String updatedTitle = uniqueTitle("Admin Updated Exam");
        mockMvc.perform(put("/api/admin/exams/{id}", examId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(updatedTitle, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(examId))
                .andExpect(jsonPath("$.data.title").value(updatedTitle))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(delete("/api/admin/exams/{id}", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Boolean enabled = jdbcTemplate.queryForObject(
                "select enabled from exams where id = ?",
                Boolean.class,
                examId
        );
        org.assertj.core.api.Assertions.assertThat(enabled).isFalse();
    }

    @Test
    void adminAddsExamProblemAndReadsResults() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = seedProblemId();
        Long studentId = userId("student");
        Long examId = createExam(token, uniqueTitle("Admin Result Exam"), true);

        mockMvc.perform(post("/api/admin/exams/{id}/problems", examId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemId": %d,
                                  "score": 40,
                                  "sortOrder": 1
                                }
                                """.formatted(problemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.problemId").value(problemId))
                .andExpect(jsonPath("$.data.score").value(40));

        Long bestSubmissionId = insertJudgedSubmission(studentId, examId, problemId, "AC", 100);
        insertJudgedSubmission(studentId, examId, problemId, "WA", 0);

        mockMvc.perform(get("/api/admin/exams/{id}/results", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.examId").value(examId))
                .andExpect(jsonPath("$.data.maxScore").value(40))
                .andExpect(jsonPath("$.data.students[0].username").value("student"))
                .andExpect(jsonPath("$.data.students[0].totalScore").value(40))
                .andExpect(jsonPath("$.data.students[0].problems[0].score").value(40))
                .andExpect(jsonPath("$.data.students[0].problems[0].bestSubmissionId").value(bestSubmissionId));
    }

    @Test
    void adminResultsIgnoreDisabledExamProblems() throws Exception {
        String token = loginAndReadToken("admin");
        Long enabledProblemId = seedProblemId();
        Long disabledProblemId = insertProblem(uniqueTitle("Disabled Admin Result Problem"), false);
        Long studentId = userId("student");
        Long examId = createExam(token, uniqueTitle("Disabled Admin Result Exam"), true);
        addExamProblem(token, examId, enabledProblemId, 40, 1)
                .andExpect(status().isOk());
        insertExamProblemDirectly(examId, disabledProblemId, 60, 2);
        insertJudgedSubmission(studentId, examId, enabledProblemId, "AC", 100);
        insertJudgedSubmission(studentId, examId, disabledProblemId, "AC", 100);

        mockMvc.perform(get("/api/admin/exams/{id}/results", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxScore").value(40))
                .andExpect(jsonPath("$.data.students[0].totalScore").value(40))
                .andExpect(jsonPath("$.data.students[0].problems.length()").value(1))
                .andExpect(jsonPath("$.data.students[0].problems[0].problemId").value(enabledProblemId));
    }

    @Test
    void disabledProblemCannotBeAddedToExam() throws Exception {
        String token = loginAndReadToken("admin");
        Long disabledProblemId = insertProblem(uniqueTitle("Disabled Add Problem"), false);
        Long examId = createExam(token, uniqueTitle("Disabled Add Exam"), true);

        addExamProblem(token, examId, disabledProblemId, 100, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void studentCannotCreateExam() throws Exception {
        String token = loginAndReadToken("student");

        mockMvc.perform(post("/api/admin/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(uniqueTitle("Student Forbidden Exam"), true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidExamCreateReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");
        OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2);
        OffsetDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);

        mockMvc.perform(post("/api/admin/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "Invalid.",
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "enabled": true
                                }
                                """.formatted(startTime, endTime)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicateExamProblemReturnsConflict() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = seedProblemId();
        Long examId = createExam(token, uniqueTitle("Duplicate Exam Problem"), true);

        addExamProblem(token, examId, problemId, 100, 1)
                .andExpect(status().isOk());

        addExamProblem(token, examId, problemId, 100, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Exam problem already exists for this exam"));
    }

    @Test
    void missingExamProblemReturnsNotFound() throws Exception {
        String token = loginAndReadToken("admin");
        Long examId = createExam(token, uniqueTitle("Missing Problem Exam"), true);

        addExamProblem(token, examId, 999999L, 100, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private org.springframework.test.web.servlet.ResultActions addExamProblem(
            String token,
            Long examId,
            Long problemId,
            int score,
            int sortOrder
    ) throws Exception {
        return mockMvc.perform(post("/api/admin/exams/{id}/problems", examId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "problemId": %d,
                          "score": %d,
                          "sortOrder": %d
                        }
                        """.formatted(problemId, score, sortOrder)));
    }

    private Long createExam(String token, String title, boolean enabled) throws Exception {
        String response = mockMvc.perform(post("/api/admin/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(title, enabled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.enabled").value(enabled))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }

    private String examJson(String title, boolean enabled) {
        OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        OffsetDateTime endTime = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        return """
                {
                  "title": "%s",
                  "description": "Admin managed exam.",
                  "startTime": "%s",
                  "endTime": "%s",
                  "enabled": %s
                }
                """.formatted(title, startTime, endTime, enabled);
    }

    private String loginAndReadToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(response, "$.data.token");
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

    private void insertExamProblemDirectly(Long examId, Long problemId, int score, int sortOrder) {
        jdbcTemplate.update(
                "insert into exam_problems (exam_id, problem_id, score, sort_order) values (?, ?, ?, ?)",
                examId,
                problemId,
                score,
                sortOrder
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

    private String uniqueTitle(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
