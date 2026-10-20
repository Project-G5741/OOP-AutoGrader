-- Persist MMD member outcomes for revisit GET /mmd (not Class correctIds).
-- Operator-run. Safe to re-run.

ALTER TABLE submission_challenge_result
    ADD COLUMN IF NOT EXISTS mmd_outcomes JSONB;
