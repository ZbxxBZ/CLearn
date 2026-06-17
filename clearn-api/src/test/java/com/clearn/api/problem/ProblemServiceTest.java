package com.clearn.api.problem;

import com.clearn.api.problem.dto.ProblemCreateRequest;
import com.clearn.api.problem.dto.ProblemDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProblemServiceTest {

    @Autowired
    private ProblemService problemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void studentProblemDetailFiltersHiddenTestCases() {
        Long problemId = seedProblemId();

        ProblemDetailResponse detail = problemService.getStudentProblem(problemId);

        assertThat(detail.samples())
                .extracting(sample -> sample.inputData())
                .containsExactly("1 2");
        assertThat(detail.samples())
                .noneMatch(sample -> sample.inputData().equals("100 200"))
                .noneMatch(sample -> sample.expectedOutput().equals("300"));
    }

    @Test
    void createProblemRejectsBlankTitle() {
        ProblemCreateRequest request = validCreateRequest(" ");

        assertThatThrownBy(() -> problemService.createProblem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void createProblemRejectsInvalidTimeLimit() {
        ProblemCreateRequest request = new ProblemCreateRequest(
                "Too Slow",
                "Description",
                "Input",
                "Output",
                "EASY",
                "basic",
                0,
                128,
                100,
                true
        );

        assertThatThrownBy(() -> problemService.createProblem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeLimitMs");
    }

    private ProblemCreateRequest validCreateRequest(String title) {
        return new ProblemCreateRequest(
                title,
                "Read two numbers and print the result.",
                "Two integers.",
                "One integer.",
                "EASY",
                "math,basic",
                1000,
                128,
                100,
                true
        );
    }

    private Long seedProblemId() {
        return jdbcTemplate.queryForObject(
                "select id from problems where title = 'A+B Problem'",
                Long.class
        );
    }
}
