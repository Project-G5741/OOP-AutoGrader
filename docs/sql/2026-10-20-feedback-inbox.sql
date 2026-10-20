-- Feedback inbox: student/lecturer Bug reports and Contributions.
-- Operator-run. Safe to re-run.

CREATE TABLE IF NOT EXISTS feedback_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    author_irn   TEXT NOT NULL,
    author_name  TEXT NOT NULL,
    item_type    TEXT NOT NULL,
    message      TEXT NOT NULL,
    status       TEXT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at        TIMESTAMPTZ,
    lecturer_response  TEXT,
    response_at        TIMESTAMPTZ,
    archived_at        TIMESTAMPTZ,
    CONSTRAINT feedback_item_type_chk CHECK (item_type IN ('BUG', 'CONTRIBUTION')),
    CONSTRAINT feedback_item_status_chk CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_feedback_item_status_created
    ON feedback_item (status, created_at DESC);

CREATE TABLE IF NOT EXISTS feedback_attachment (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_item_id     UUID NOT NULL REFERENCES feedback_item(id) ON DELETE CASCADE,
    original_filename    TEXT NOT NULL,
    stored_content_type  TEXT NOT NULL,
    byte_size            INTEGER NOT NULL,
    content              BYTEA NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feedback_attachment_item
    ON feedback_attachment (feedback_item_id);

ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS lecturer_response TEXT;
ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS response_at TIMESTAMPTZ;
ALTER TABLE feedback_item ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ;
