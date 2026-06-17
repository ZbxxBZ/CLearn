package com.clearn.api.rate;

import com.clearn.api.submission.JudgeTaskPublisher;
import com.clearn.api.submission.dto.SubmissionCreateRequest;
import com.clearn.common.json.JsonMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clearn.submission.rate-limit.enabled=true",
        "clearn.submission.rate-limit.max-requests=3",
        "clearn.submission.rate-limit.window=PT10S"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionRateLimitFilterTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    private final ObjectMapper objectMapper = JsonMappers.objectMapper();
    private final Map<String, Long> counters = new ConcurrentHashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpRedisCounter() {
        counters.clear();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return counters.merge(key, 1L, Long::sum);
        });
        doAnswer(invocation -> null)
                .when(redisTemplate)
                .expire(any(String.class), any(Duration.class));
    }

    @Test
    void returnsTooManyRequestsWhenSubmissionLimitExceeded() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(sourceCode(i)))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/problems/{id}/submissions", problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(sourceCode(4)))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("submission rate limit exceeded"));
    }

    @Test
    void returnsTooManyRequestsWhenExamSubmissionLimitExceeded() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(problemId);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/exams/{examId}/problems/{problemId}/submissions", examId, problemId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(sourceCode(i)))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/exams/{examId}/problems/{problemId}/submissions", examId, problemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmissionCreateRequest(sourceCode(4)))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("submission rate limit exceeded"));
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

    private Long insertExamWithProblem(Long problemId) {
        LocalDateTime startTime = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        LocalDateTime endTime = LocalDateTime.now(ZoneOffset.UTC).plusHours(1);
        jdbcTemplate.update(
                """
                        insert into exams (title, description, start_time, end_time, enabled)
                        values ('Rate Limit Exam', 'Exam for rate limit tests.', ?, ?, true)
                        """,
                startTime,
                endTime
        );
        Long examId = jdbcTemplate.queryForObject(
                "select id from exams where title = 'Rate Limit Exam'",
                Long.class
        );
        jdbcTemplate.update(
                "insert into exam_problems (exam_id, problem_id, score, sort_order) values (?, ?, 100, 1)",
                examId,
                problemId
        );
        return examId;
    }

    private String sourceCode(int value) {
        return """
                #include <stdio.h>
                int main(void) {
                    printf("%d\\n", %d);
                    return 0;
                }
                """.formatted(value, value);
    }
}
