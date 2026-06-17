package com.clearn.api.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsCoreTablesAndSeedProblem() {
        Integer problemCount = jdbcTemplate.queryForObject("select count(*) from problems", Integer.class);
        Integer userCount = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
        Integer testCaseCount = jdbcTemplate.queryForObject("select count(*) from test_cases", Integer.class);

        assertThat(problemCount).isGreaterThanOrEqualTo(1);
        assertThat(userCount).isGreaterThanOrEqualTo(2);
        assertThat(testCaseCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void createsAllCoreTables() {
        assertTableExists("users");
        assertTableExists("problems");
        assertTableExists("test_cases");
        assertTableExists("submissions");
        assertTableExists("exams");
        assertTableExists("exam_problems");
    }

    @Test
    void rejectsDuplicateTestCaseSortOrderForSameProblem() {
        Long problemId = jdbcTemplate.queryForObject(
                "select id from problems where title = 'A+B Problem'",
                Long.class
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into test_cases (problem_id, input_data, expected_output, sample, sort_order) values (?, ?, ?, ?, ?)",
                problemId,
                "2 2",
                "4",
                false,
                1
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTestCaseForMissingProblem() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into test_cases (problem_id, input_data, expected_output, sample, sort_order) values (?, ?, ?, ?, ?)",
                -1L,
                "2 2",
                "4",
                false,
                99
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertTableExists(String tableName) {
        Integer rowCount = jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);

        assertThat(rowCount).isNotNull();
    }
}
