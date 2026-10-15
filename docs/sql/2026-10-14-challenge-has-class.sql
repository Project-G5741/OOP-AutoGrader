-- Operator-run migration: add has_class so Declaration (class) grading can be disabled per challenge.

ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS has_class BOOLEAN NOT NULL DEFAULT true;
