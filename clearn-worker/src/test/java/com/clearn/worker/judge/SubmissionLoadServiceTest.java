package com.clearn.worker.judge;

import com.clearn.common.enums.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubmissionLoadServiceTest {
    private JdbcTemplate jdbcTemplate;
    private SubmissionLoadService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:worker_load_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        service = new SubmissionLoadService(jdbcTemplate);

        jdbcTemplate.execute("""
                create table problems (
                    id bigint primary key,
                    title varchar(200) not null,
                    time_limit_ms int not null,
                    memory_limit_mb int not null,
                    score int not null
                )
                """);
        jdbcTemplate.execute("""
                create table submissions (
                    id bigint primary key,
                    problem_id bigint not null,
                    exam_id bigint,
                    language varchar(32) not null,
                    source_code text not null
                )
                """);
        jdbcTemplate.execute("""
                create table exam_problems (
                    exam_id bigint not null,
                    problem_id bigint not null,
                    score int not null
                )
                """);
        jdbcTemplate.execute("""
                create table test_cases (
                    id bigint primary key,
                    problem_id bigint not null,
                    input_data text not null,
                    expected_output text not null,
                    sample boolean not null,
                    sort_order int not null
                )
                """);
        jdbcTemplate.update("""
                insert into problems (id, title, time_limit_ms, memory_limit_mb, score)
                values (3001, 'A+B Problem', 1500, 64, 100)
                """);
        jdbcTemplate.update("""
                insert into submissions (id, problem_id, exam_id, language, source_code)
                values (10001, 3001, null, 'C', '#include <stdio.h>\\nint main(void){int a,b;scanf("%d %d",&a,&b);printf("%d\\\\n", a+b);return 0;}')
                """);
        jdbcTemplate.update("""
                insert into test_cases (id, problem_id, input_data, expected_output, sample, sort_order)
                values (3, 3001, '5 8', '13', false, 30)
                """);
        jdbcTemplate.update("""
                insert into test_cases (id, problem_id, input_data, expected_output, sample, sort_order)
                values (1, 3001, '1 2', '3', true, 10)
                """);
        jdbcTemplate.update("""
                insert into test_cases (id, problem_id, input_data, expected_output, sample, sort_order)
                values (2, 3001, '100 200', '300', false, 20)
                """);
    }

    @Test
    void loadReadsSourceProblemLimitsAndJudgeTestCasesSortedBySortOrder() {
        SubmissionLoadService.LoadedSubmission loaded = service.load(10001L);

        assertThat(loaded.submissionId()).isEqualTo(10001L);
        assertThat(loaded.problemId()).isEqualTo(3001L);
        assertThat(loaded.language()).isEqualTo(Language.C);
        assertThat(loaded.sourceCode()).contains("scanf");
        assertThat(loaded.timeLimitMs()).isEqualTo(1500);
        assertThat(loaded.memoryLimitMb()).isEqualTo(64);
        assertThat(loaded.maxScore()).isEqualTo(100);
        assertThat(loaded.testCases())
                .extracting(SubmissionLoadService.LoadedTestCase::sortOrder)
                .containsExactly(20, 30);
        assertThat(loaded.testCases())
                .extracting(SubmissionLoadService.LoadedTestCase::inputData)
                .containsExactly("100 200", "5 8");
        assertThat(loaded.testCases())
                .noneMatch(SubmissionLoadService.LoadedTestCase::sample);
    }
}
