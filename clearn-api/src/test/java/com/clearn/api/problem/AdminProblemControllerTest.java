package com.clearn.api.problem;

import com.jayway.jsonpath.JsonPath;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
                                  "enabled": true,
                                  "judgeCases": [
                                    {"inputData": "1 2", "expectedOutput": "3", "sortOrder": 1},
                                    {"inputData": "2 3", "expectedOutput": "5", "sortOrder": 2},
                                    {"inputData": "-1 9", "expectedOutput": "8", "sortOrder": 3},
                                    {"inputData": "0 0", "expectedOutput": "0", "sortOrder": 4},
                                    {"inputData": "100 200", "expectedOutput": "300", "sortOrder": 5}
                                  ],
                                  "samples": [
                                    {"inputData": "1 2", "expectedOutput": "3"}
                                  ]
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
                                  "enabled": true,
                                  "judgeCases": [
                                    {"inputData": "1 2", "expectedOutput": "3", "sortOrder": 1},
                                    {"inputData": "2 3", "expectedOutput": "5", "sortOrder": 2},
                                    {"inputData": "-1 9", "expectedOutput": "8", "sortOrder": 3},
                                    {"inputData": "0 0", "expectedOutput": "0", "sortOrder": 4},
                                    {"inputData": "100 200", "expectedOutput": "300", "sortOrder": 5}
                                  ]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminImportsProblemsFromExcelWithFiveJudgeCasesAndOptionalSample() throws Exception {
        String token = loginAndReadToken("admin");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "problems.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                importWorkbook()
        );

        mockMvc.perform(multipart("/api/admin/problems/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.problemIds[0]").exists());

        Long problemId = jdbcTemplate.queryForObject(
                "select id from problems where title = 'Excel Sum'",
                Long.class
        );
        Integer judgeCaseCount = jdbcTemplate.queryForObject(
                "select count(*) from test_cases where problem_id = ? and sample = false",
                Integer.class,
                problemId
        );
        Integer sampleCount = jdbcTemplate.queryForObject(
                "select count(*) from test_cases where problem_id = ? and sample = true",
                Integer.class,
                problemId
        );

        org.assertj.core.api.Assertions.assertThat(judgeCaseCount).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(sampleCount).isEqualTo(1);
    }

    @Test
    void adminAddsSampleTestCaseSuccessfully() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Add Sample Fixture");

        mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "10 20",
                                  "expectedOutput": "30",
                                  "sample": true,
                                  "sortOrder": 2001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.sample").value(true));
    }

    @Test
    void addingSixthJudgeTestCaseReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Sixth Judge Fixture");

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("problem must contain exactly 5 judge test cases"));
    }

    @Test
    void deletingJudgeTestCaseReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Delete Judge Fixture");
        Long testCaseId = judgeTestCaseId(problemId);

        mockMvc.perform(delete("/api/admin/test-cases/{id}", testCaseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("problem must contain exactly 5 judge test cases"));
    }

    @Test
    void updatingSampleToSixthJudgeTestCaseReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Sample To Judge Fixture");
        Long testCaseId = createSampleTestCase(token, problemId, "20 30", "50", 200);

        mockMvc.perform(put("/api/admin/test-cases/{id}", testCaseId)
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("problem must contain exactly 5 judge test cases"));
    }

    @Test
    void updatingJudgeTestCaseToSampleReturnsBadRequest() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Judge To Sample Fixture");
        Long testCaseId = judgeTestCaseId(problemId);

        mockMvc.perform(put("/api/admin/test-cases/{id}", testCaseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "1 2",
                                  "expectedOutput": "3",
                                  "sample": true,
                                  "sortOrder": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("problem must contain exactly 5 judge test cases"));
    }

    @Test
    void duplicateTestCaseSortOrderReturnsConflict() throws Exception {
        String token = loginAndReadToken("admin");
        Long problemId = createProblemFixture("Admin Duplicate Sort Fixture");

        createSampleTestCase(token, problemId, "20 30", "50", 200);

        mockMvc.perform(post("/api/admin/problems/{id}/test-cases", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "30 40",
                                  "expectedOutput": "70",
                                  "sample": true,
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
        Long problemId = createProblemFixture("Admin Duplicate Sort Update Fixture");
        createSampleTestCase(token, problemId, "40 50", "90", 210);
        Long testCaseId = createSampleTestCase(token, problemId, "50 60", "110", 211);

        mockMvc.perform(put("/api/admin/test-cases/{id}", testCaseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inputData": "50 60",
                                  "expectedOutput": "110",
                                  "sample": true,
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

    @Test
    void adminImportsProblemsFromExcelWithoutSample() throws Exception {
        String token = loginAndReadToken("admin");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "problems-without-sample.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                importWorkbookWithoutSample()
        );

        mockMvc.perform(multipart("/api/admin/problems/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.importedCount").value(1));

        Long problemId = jdbcTemplate.queryForObject(
                "select id from problems where title = 'Excel No Sample Sum'",
                Long.class
        );
        Integer judgeCaseCount = jdbcTemplate.queryForObject(
                "select count(*) from test_cases where problem_id = ? and sample = false",
                Integer.class,
                problemId
        );
        Integer sampleCount = jdbcTemplate.queryForObject(
                "select count(*) from test_cases where problem_id = ? and sample = true",
                Integer.class,
                problemId
        );

        org.assertj.core.api.Assertions.assertThat(judgeCaseCount).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(sampleCount).isZero();
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

    private Long judgeTestCaseId(Long problemId) {
        return jdbcTemplate.queryForObject(
                """
                select id
                from test_cases
                where problem_id = ?
                  and sample = false
                order by sort_order
                limit 1
                """,
                Long.class,
                problemId
        );
    }

    private Long createProblemFixture(String title) {
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
                values (?, 'Fixture problem.', 'Input.', 'Output.', 'EASY', 'fixture', 1000, 128, 100, true)
                """,
                title
        );
        Long problemId = jdbcTemplate.queryForObject(
                "select max(id) from problems where title = ?",
                Long.class,
                title
        );
        for (int index = 1; index <= 5; index++) {
            jdbcTemplate.update(
                    """
                    insert into test_cases (problem_id, input_data, expected_output, sample, sort_order)
                    values (?, ?, ?, false, ?)
                    """,
                    problemId,
                    index + " " + index,
                    String.valueOf(index + index),
                    index
            );
        }
        return problemId;
    }

    private Long createSampleTestCase(
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
                                  "sample": true,
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

    private byte[] importWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("problems");
            var header = sheet.createRow(0);
            String[] headers = {
                    "title", "description", "inputDescription", "outputDescription", "difficulty", "tags",
                    "timeLimitMs", "memoryLimitMb", "score", "enabled",
                    "sampleInput", "sampleOutput",
                    "case1Input", "case1Output", "case2Input", "case2Output", "case3Input", "case3Output",
                    "case4Input", "case4Output", "case5Input", "case5Output"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            var row = sheet.createRow(1);
            String[] values = {
                    "Excel Sum", "Read two integers and output their sum.", "Two integers.", "One integer.",
                    "EASY", "math,excel", "1000", "128", "100", "true",
                    "1 2", "3",
                    "1 2", "3", "2 3", "5", "-1 9", "8", "0 0", "0", "100 200", "300"
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] importWorkbookWithoutSample() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("problems");
            var header = sheet.createRow(0);
            String[] headers = {
                    "title", "description", "inputDescription", "outputDescription", "difficulty", "tags",
                    "timeLimitMs", "memoryLimitMb", "score", "enabled",
                    "case1Input", "case1Output", "case2Input", "case2Output", "case3Input", "case3Output",
                    "case4Input", "case4Output", "case5Input", "case5Output"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            var row = sheet.createRow(1);
            String[] values = {
                    "Excel No Sample Sum", "Read two integers and output their sum.", "Two integers.", "One integer.",
                    "EASY", "math,excel", "1000", "128", "100", "true",
                    "1 2", "3", "2 3", "5", "-1 9", "8", "0 0", "0", "100 200", "300"
            };
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(values[i]);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
