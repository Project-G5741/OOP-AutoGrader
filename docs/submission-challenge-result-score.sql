-- Operator-run script: add per-challenge score to submission_challenge_result.
-- Run against the project PostgreSQL database before deploying the backend change.

ALTER TABLE submission_challenge_result
    ADD COLUMN IF NOT EXISTS score NUMERIC(6, 2) NOT NULL DEFAULT 0;
