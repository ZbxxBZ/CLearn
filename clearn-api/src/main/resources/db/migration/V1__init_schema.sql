CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    input_description TEXT,
    output_description TEXT,
    difficulty VARCHAR(32) NOT NULL,
    tags VARCHAR(255),
    time_limit_ms INT NOT NULL DEFAULT 1000,
    memory_limit_mb INT NOT NULL DEFAULT 256,
    score INT NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Application write paths are responsible for maintaining updated_at.
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    problem_id BIGINT NOT NULL,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    sample BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_cases_problem
        FOREIGN KEY (problem_id) REFERENCES problems (id),
    CONSTRAINT uk_test_cases_problem_sort
        UNIQUE (problem_id, sort_order)
);

CREATE TABLE exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    exam_id BIGINT,
    language VARCHAR(32) NOT NULL,
    source_code TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    score INT NOT NULL DEFAULT 0,
    passed_test_cases INT,
    total_test_cases INT,
    time_used_ms INT,
    memory_used_kb INT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    judged_at TIMESTAMP,
    CONSTRAINT fk_submissions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_submissions_problem
        FOREIGN KEY (problem_id) REFERENCES problems (id),
    CONSTRAINT fk_submissions_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id)
);

CREATE TABLE exam_problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 100,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exam_problems_exam
        FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_exam_problems_problem
        FOREIGN KEY (problem_id) REFERENCES problems (id),
    CONSTRAINT uk_exam_problems_exam_problem
        UNIQUE (exam_id, problem_id)
);

CREATE INDEX idx_test_cases_problem_id ON test_cases (problem_id);
CREATE INDEX idx_submissions_user_created_at ON submissions (user_id, created_at);
CREATE INDEX idx_submissions_problem_id ON submissions (problem_id);
CREATE INDEX idx_exam_problems_exam_id ON exam_problems (exam_id);
