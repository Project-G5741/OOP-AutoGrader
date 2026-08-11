-- Operator-run migration: add is_hidden to rubric testcase rows for student I/O card visibility.

ALTER TABLE testcase
    ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT false;
