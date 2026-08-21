-- Current-term flag. Lecturer sets which term is "now".
-- Operator-run. Safe to re-run.

ALTER TABLE term
    ADD COLUMN IF NOT EXISTS is_current BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_term_is_current
    ON term (is_current)
    WHERE is_current;
