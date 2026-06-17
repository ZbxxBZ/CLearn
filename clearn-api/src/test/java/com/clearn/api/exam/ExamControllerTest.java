package com.clearn.api.exam;

import com.clearn.api.submission.JudgeTaskPublisher;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void studentListsOpenExamsAndReadsDetail() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Controller Exam",
                problemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                100
        );

        mockMvc.perform(get("/api/exams").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].title", hasItem("Controller Exam")));

        mockMvc.perform(get("/api/exams/{id}", examId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(examId))
                .andExpect(jsonPath("$.data.problems[0].problemId").value(problemId))
                .andExpect(jsonPath("$.data.problems[0].score").value(100));
    }

    @Test
    void studentCreatesExamSubmissionAndReadsMyResult() throws Exception {
        String token = loginAndReadToken("student");
        Long userId = userId("student");
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Controller Submit Exam",
                problemId,
                nowUtc().minusHours(1),
                nowUtc().plusHours(1),
                true,
                100
        );

        mockMvc.perform(post("/api/exams/{examId}/problems/{problemId}/submissions", examId, problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceCode":"int main(void) { return 0; }"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Long submissionId = jdbcTemplate.queryForObject(
                "select max(id) from submissions where user_id = ? and exam_id = ? and problem_id = ?",
                Long.class,
                userId,
                examId,
                problemId
        );
        jdbcTemplate.update(
                "update submissions set status = 'AC', score = 100, judged_at = CURRENT_TIMESTAMP where id = ?",
                submissionId
        );

        mockMvc.perform(get("/api/exams/{id}/my-result", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(100))
                .andExpect(jsonPath("$.data.problems[0].bestSubmissionId").value(submissionId));
    }

    @Test
    void unauthenticatedExamRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());
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
        jdbcTemplate.update(
                "insert into exam_problems (exam_id, problem_id, score, sort_order) values (?, ?, ?, 1)",
                examId,
                problemId,
                score
        );
        return examId;
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
