-- Operator-run migration: add has_mmd to challenge rows so MMD grading can be disabled per challenge.

ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS has_mmd BOOLEAN NOT NULL DEFAULT true;
