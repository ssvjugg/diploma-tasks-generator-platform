ALTER TABLE submission
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE submission
    DROP CONSTRAINT chk_submission_status;

UPDATE submission
SET status = CASE status
    WHEN 'PENDING' THEN 'QUEUED'
    WHEN 'RUNNING' THEN 'PROCESSING'
    WHEN 'ERROR' THEN 'FAILED'
    ELSE status
END;

ALTER TABLE submission
    ADD COLUMN max_score INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN error_message TEXT;

ALTER TABLE submission
    ADD CONSTRAINT chk_submission_status
        CHECK (status IN (
            'QUEUED',
            'PROCESSING',
            'ACCEPTED',
            'WRONG_ANSWER',
            'COMPILATION_ERROR',
            'RUNTIME_ERROR',
            'TIME_LIMIT_EXCEEDED',
            'MEMORY_LIMIT_EXCEEDED',
            'FAILED'
        ));

ALTER TABLE submission
    ADD CONSTRAINT chk_submission_max_score
        CHECK (max_score >= 0);

INSERT INTO programming_language (name, code, judge0_language_id)
VALUES
    ('Python 3', 'python', 71),
    ('Java 21', 'java', 91),
    ('JavaScript Node.js', 'javascript', 63),
    ('C++', 'cpp', 54),
    ('C', 'c', 50)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    judge0_language_id = EXCLUDED.judge0_language_id;

ALTER TABLE submission_test_result
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE submission_test_result
    DROP CONSTRAINT chk_submission_test_result_status;

UPDATE submission_test_result
SET status = CASE status
    WHEN 'TIME_LIMIT' THEN 'TIME_LIMIT_EXCEEDED'
    WHEN 'MEMORY_LIMIT' THEN 'MEMORY_LIMIT_EXCEEDED'
    ELSE status
END;

ALTER TABLE submission_test_result
    ADD COLUMN test_case_index INTEGER,
    ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN input_data TEXT,
    ADD COLUMN expected_output TEXT,
    ADD COLUMN points INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN time NUMERIC(10, 3),
    ADD COLUMN memory INTEGER;

UPDATE submission_test_result result
SET is_hidden = test_case.is_hidden,
    input_data = test_case.input_data,
    expected_output = test_case.expected_output,
    points = test_case.points
FROM test_case
WHERE result.test_case_id = test_case.id;

WITH ranked_results AS (
    SELECT id,
           row_number() OVER (PARTITION BY submission_id ORDER BY created_at, id) - 1 AS test_index
    FROM submission_test_result
)
UPDATE submission_test_result result
SET test_case_index = ranked_results.test_index
FROM ranked_results
WHERE result.id = ranked_results.id;

ALTER TABLE submission_test_result
    ALTER COLUMN test_case_index SET NOT NULL;

ALTER TABLE submission_test_result
    DROP CONSTRAINT fk_submission_test_result_test_case;

ALTER TABLE submission_test_result
    ALTER COLUMN test_case_id DROP NOT NULL;

ALTER TABLE submission_test_result
    ADD CONSTRAINT fk_submission_test_result_test_case
        FOREIGN KEY (test_case_id) REFERENCES test_case (id) ON DELETE SET NULL;

ALTER TABLE submission_test_result
    ADD CONSTRAINT uq_submission_test_result_index
        UNIQUE (submission_id, test_case_index);

ALTER TABLE submission_test_result
    ADD CONSTRAINT chk_submission_test_result_status
        CHECK (status IN (
            'QUEUED',
            'PROCESSING',
            'ACCEPTED',
            'WRONG_ANSWER',
            'COMPILATION_ERROR',
            'RUNTIME_ERROR',
            'TIME_LIMIT_EXCEEDED',
            'MEMORY_LIMIT_EXCEEDED',
            'FAILED'
        ));

ALTER TABLE submission_test_result
    ADD CONSTRAINT chk_submission_test_result_points
        CHECK (points >= 0);
