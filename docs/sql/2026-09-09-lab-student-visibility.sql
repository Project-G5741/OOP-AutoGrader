-- Per-lab student visibility and scheduled release date.
-- Apply manually in dev/prod PostgreSQL (no Flyway in repo).

ALTER TABLE lab
    ADD COLUMN IF NOT EXISTS student_visible BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE lab
    ADD COLUMN IF NOT EXISTS release_date DATE NULL;
