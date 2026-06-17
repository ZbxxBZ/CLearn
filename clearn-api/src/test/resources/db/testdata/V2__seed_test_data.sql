INSERT INTO users (username, password_hash, role, enabled)
VALUES
    ('admin', '$2a$10$75AsrcqQNYrMUWKrpyGPqeSXSi8f79mdBr4M94ps.T4uAj9ISIuRe', 'ADMIN', TRUE),
    ('student', '$2a$10$75AsrcqQNYrMUWKrpyGPqeSXSi8f79mdBr4M94ps.T4uAj9ISIuRe', 'STUDENT', TRUE);

INSERT INTO problems (
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
VALUES (
    'A+B Problem',
    'Given two integers a and b, output their sum.',
    'Two integers a and b separated by whitespace.',
    'A single integer, the sum of a and b.',
    'EASY',
    'math,basic',
    1000,
    128,
    100,
    TRUE
);

INSERT INTO test_cases (problem_id, input_data, expected_output, sample, sort_order)
VALUES
    ((SELECT id FROM problems WHERE title = 'A+B Problem'), '1 2', '3', TRUE, 1),
    ((SELECT id FROM problems WHERE title = 'A+B Problem'), '100 200', '300', FALSE, 2);
