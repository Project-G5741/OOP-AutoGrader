-- Soft-archive Resolved feedback that has a lecturer reply (author bell keeps uncleared replies).
-- Operator-run. Safe to re-run.

ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;
