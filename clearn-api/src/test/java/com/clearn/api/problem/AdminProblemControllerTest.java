package com.clearn.api.problem;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminCreatesProblemSuccessfully() throws Exception {
        String token = loginAndReadToken("admin");

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Maximum",
                                  "description": "Read two integers and print the larger one.",
                                  "inputDescription": "Two integers.",
                                  "outputDescription": "The larger integer.",
                                  "difficulty": "EASY",
                                  "tags": "branch,basic",
                                  "timeLimitMs": 1000,
                                  "memoryLimitMb": 128,
                                  "score": 100,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void studentCannotCreateProblem() throws Exception {
        String token = loginAndReadToken("student");

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Student Problem",
                                  "description": "Not allowed.",
                                  "inputDescription": "Input.",
                                  "outputDescription": "Output.",
                                  "difficulty": "EASY",
                                  "tags": "security",
                                  "timeLimitMs": 1000,
                                  "memoryLimitMb": 128,
                                  "score": 100,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAddsHiddenTestCaseSuccessfully() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = seedProblemId();

        mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "10 20",
                                  "expectedOutput": "30",
                                  "sample": false,
                                  "sortOrder": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.sample").value(false));
    }

    @Test
    void duplicateTestCaseSortOrderReturnsConflict() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = seedProblemId();

        mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "20 30",
                                  "expectedOutput": "50",
                                  "sample": false,
                                  "sortOrder": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "30 40",
                                  "expectedOutput": "70",
                                  "sample": false,
                                  "sortOrder": 200
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Test case sort order already exists for this problem"));
    }

    @Test
    void duplicateTestCaseSortOrderOnUpdateReturnsConflict() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = seedProblemId();
        createTestCase(token, problemId, "40 50", "90", 210);
        Long testCaseId = createTestCase(token, problemId, "50 60", "110", 211);

        mockMvc.perform(put("/api/admin/test-cases/{id}", testCaseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "50 60",
                                  "expectedOutput": "110",
                                  "sample": false,
                                  "sortOrder": 210
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Test case sort order already exists for this problem"));
    }

    @Test
    void updateMissingProblemReturnsNotFound() throws Exception {
        String token = loginAndReadToken("admin");

        mockMvc.perform(put("/api/admin/problems/{id}", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing",
                                  "description": "No such problem.",
                                  "inputDescription": "Input.",
                                  "outputDescription": "Output.",
                                  "difficulty": "EASY",
                                  "tags": "missing",
                                  "timeLimitMs": 1000,
                                  "memoryLimitMb": 128,
                                  "score": 100,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteMissingTestCaseReturnsNotFound() throws Exception {
        String token = loginAndReadToken("admin");

        mockMvc.perform(delete("/api/admin/test-cases/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidProblemCreateReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "Invalid.",
                                  "inputDescription": "Input.",
                                  "outputDescription": "Output.",
                                  "difficulty": "EASY",
                                  "tags": "invalid",
                                  "timeLimitMs": 0,
                                  "memoryLimitMb": 128,
                                  "score": 100,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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

    private Long createTestCase(
            String token,
            Long problemId,
            String inputData,
            String expectedOutput,
            int sortOrder
    ) throws Exception {
        String response = mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "%s",
                                  "expectedOutput": "%s",
                                  "sample": false,
                                  "sortOrder": %d
                                }
                                """.formatted(inputData, expectedOutput, sortOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }
}
