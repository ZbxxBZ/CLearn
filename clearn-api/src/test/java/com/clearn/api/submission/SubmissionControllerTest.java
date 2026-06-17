package com.clearn.api.submission;

import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.clearn.common.json.JsonMappers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMappers.objectMapper();

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void studentSubmitsAbProblemSuccessfully() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();

        mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(abSourceCode()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submissionId").exists())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void unauthenticatedSubmitReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/problems/{id}/submissions", seedProblemId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(abSourceCode()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentReadsOwnSubmissionByIdSuccessfully() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();
        Long submissionId = createSubmission(token, problemId);

        mockMvc.perform(get("/api/submissions/{id}", submissionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(submissionId))
                .andExpect(jsonPath("$.data.problemId").value(problemId))
                .andExpect(jsonPath("$.data.language").value("C"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void studentListsOwnSubmissionsSuccessfully() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();
        Long submissionId = createSubmission(token, problemId);

        mockMvc.perform(get("/api/submissions/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(submissionId))
                .andExpect(jsonPath("$.data[0].problemId").value(problemId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void studentCannotReadAnotherUsersSubmission() throws Exception {
        String studentToken = loginAndReadToken("student");
        Long otherSubmissionId = insertSubmissionForUser(userId("admin"), seedProblemId());

        mockMvc.perform(get("/api/submissions/{id}", otherSubmissionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanReadAnySubmission() throws Exception {
        String adminToken = loginAndReadToken("admin");
        Long otherSubmissionId = insertSubmissionForUser(userId("student"), seedProblemId());

        mockMvc.perform(get("/api/submissions/{id}", otherSubmissionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(otherSubmissionId))
                .andExpect(jsonPath("$.data.problemId").exists());
    }

    private Long createSubmission(String token, Long problemId) throws Exception {
        String response = mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(abSourceCode()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number submissionId = JsonPath.read(response, "$.data.submissionId");
        return submissionId.longValue();
    }

    private Long insertSubmissionForUser(Long userId, Long problemId) {
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
                            score
                        )
                        values (?, ?, null, 'C', ?, 'PENDING', 0)
                        """,
                userId,
                problemId,
                sourceCode
        );
        return jdbcTemplate.queryForObject(
                "select id from submissions where source_code = ?",
                Long.class,
                sourceCode
        );
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

    private String abSourceCode() {
        return """
                #include <stdio.h>
                int main(void) {
                    int a, b;
                    scanf("%d %d", &a, &b);
                    printf("%d\\n", a + b);
                    return 0;
                }
                """;
    }
}
