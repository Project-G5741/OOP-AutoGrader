-- Lecturer short reply on resolved Feedback items (student notification).
-- Operator-run. Safe to re-run.

ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS lecturer_response TEXT;
ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS response_at TIMESTAMPTZ;
