ALTER TABLE generation_request
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE generation_request
    DROP CONSTRAINT chk_generation_request_status;

UPDATE generation_request
SET status = 'COMPLETED'
WHERE status = 'DONE';

ALTER TABLE generation_request
    ADD CONSTRAINT chk_generation_request_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED'));

ALTER TABLE generation_request
    ADD COLUMN generated_draft JSONB;
