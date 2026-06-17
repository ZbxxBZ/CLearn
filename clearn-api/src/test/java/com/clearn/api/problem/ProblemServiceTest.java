package com.clearn.api.problem;

import com.clearn.api.problem.dto.ProblemCreateRequest;
import com.clearn.api.problem.dto.ProblemDetailResponse;
import com.clearn.api.problem.dto.TestCaseCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
    void createProblemPersistsFiveJudgeCasesAndOptionalSampleSeparately() {
        ProblemCreateRequest request = validCreateRequest(
                "Sum With Five Cases",
                fiveJudgeCases(),
                List.of(new TestCaseCreateRequest("1 2", "3", true, 1001))
        );

        Long problemId = problemService.createProblem(request);

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
        ProblemDetailResponse detail = problemService.getStudentProblem(problemId);

        assertThat(judgeCaseCount).isEqualTo(5);
        assertThat(sampleCount).isEqualTo(1);
        assertThat(detail.samples())
                .extracting(sample -> sample.inputData())
                .containsExactly("1 2");
    }

    @Test
    void createProblemRejectsJudgeCaseCountOtherThanFive() {
        ProblemCreateRequest request = validCreateRequest(
                "Too Few Cases",
                fiveJudgeCases().subList(0, 4),
                List.of()
        );

        assertThatThrownBy(() -> problemService.createProblem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 5 judge test cases");
    }

    @Test
    void createProblemRejectsBlankTitle() {
        ProblemCreateRequest request = validCreateRequest(" ", fiveJudgeCases(), List.of());

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
                true,
                fiveJudgeCases(),
                List.of()
        );

        assertThatThrownBy(() -> problemService.createProblem(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeLimitMs");
    }

    private ProblemCreateRequest validCreateRequest(
            String title,
            List<TestCaseCreateRequest> judgeCases,
            List<TestCaseCreateRequest> samples
    ) {
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
                true,
                judgeCases,
                samples
        );
    }

    private List<TestCaseCreateRequest> fiveJudgeCases() {
        return List.of(
                new TestCaseCreateRequest("1 2", "3", false, 1),
                new TestCaseCreateRequest("10 20", "30", false, 2),
                new TestCaseCreateRequest("-5 8", "3", false, 3),
                new TestCaseCreateRequest("0 0", "0", false, 4),
                new TestCaseCreateRequest("100 200", "300", false, 5)
        );
    }

    private Long seedProblemId() {
        return jdbcTemplate.queryForObject(
                "select id from problems where title = 'A+B Problem'",
                Long.class
        );
    }
}
