package com.clearn.worker.judge;

import com.clearn.common.enums.Language;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SubmissionLoadService {
    private static final String LOAD_SUBMISSION_SQL = """
            select s.id as submissionId,
                   s.problem_id as problemId,
                   s.language as language,
                   s.source_code as sourceCode,
                   p.time_limit_ms as timeLimitMs,
                   p.memory_limit_mb as memoryLimitMb
            from submissions s
            join problems p on p.id = s.problem_id
            where s.id = ?
            """;

    private static final String LOAD_TEST_CASE_SQL = """
            select id,
                   problem_id as problemId,
                   input_data as inputData,
                   expected_output as expectedOutput,
                   sample,
                   sort_order as sortOrder
            from test_cases
            where problem_id = ?
              and sample = false
            order by sort_order, id
            """;

    private final JdbcTemplate jdbcTemplate;

    public SubmissionLoadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LoadedSubmission load(Long submissionId) {
        LoadedSubmissionHeader header = jdbcTemplate.query(LOAD_SUBMISSION_SQL, rs -> {
            if (!rs.next()) {
                return null;
            }
            return new LoadedSubmissionHeader(
                    rs.getLong("submissionId"),
                    rs.getLong("problemId"),
                    rs.getString("language"),
                    rs.getString("sourceCode"),
                    rs.getInt("timeLimitMs"),
                    rs.getInt("memoryLimitMb")
            );
        }, submissionId);
        if (header == null) {
            throw new NoSuchElementException("submission not found: " + submissionId);
        }

        List<LoadedTestCase> testCases = jdbcTemplate.query(
                LOAD_TEST_CASE_SQL,
                (rs, rowNum) -> new LoadedTestCase(
                        rs.getLong("id"),
                        rs.getLong("problemId"),
                        rs.getString("inputData"),
                        rs.getString("expectedOutput"),
                        rs.getBoolean("sample"),
                        rs.getInt("sortOrder")
                ),
                header.problemId()
        );

        return new LoadedSubmission(
                header.submissionId(),
                header.problemId(),
                Language.valueOf(header.language()),
                header.sourceCode(),
                header.timeLimitMs(),
                header.memoryLimitMb(),
                List.copyOf(testCases)
        );
    }

    private record LoadedSubmissionHeader(
            Long submissionId,
            Long problemId,
            String language,
            String sourceCode,
            Integer timeLimitMs,
            Integer memoryLimitMb
    ) {
    }

    public record LoadedSubmission(
            Long submissionId,
            Long problemId,
            Language language,
            String sourceCode,
            Integer timeLimitMs,
            Integer memoryLimitMb,
            List<LoadedTestCase> testCases
    ) {
    }

    public record LoadedTestCase(
            Long id,
            Long problemId,
            String inputData,
            String expectedOutput,
            Boolean sample,
            Integer sortOrder
    ) {
    }
}
