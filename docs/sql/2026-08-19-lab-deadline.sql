-- Per-lab submission deadline and reminder email ledger.
-- Apply manually in dev/prod PostgreSQL (no Flyway in repo).

ALTER TABLE lab
    ADD COLUMN IF NOT EXISTS deadline_date DATE NULL;

CREATE TABLE IF NOT EXISTS lab_deadline_email_sent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    threshold_hours SMALLINT NOT NULL CHECK (threshold_hours IN (72, 24)),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (lab_id, user_id, threshold_hours)
);

CREATE INDEX IF NOT EXISTS idx_lab_deadline_email_sent_lab_threshold
    ON lab_deadline_email_sent (lab_id, threshold_hours);
