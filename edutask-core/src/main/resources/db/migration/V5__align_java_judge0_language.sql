INSERT INTO programming_language (name, code, judge0_language_id)
VALUES ('Java', 'java', 62)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    judge0_language_id = EXCLUDED.judge0_language_id;
