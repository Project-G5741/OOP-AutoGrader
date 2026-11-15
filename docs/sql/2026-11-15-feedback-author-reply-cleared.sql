-- Operator-run: author-cleared reply watermark on feedback_item.
-- Startup migrator FeedbackAuthorReplyClearedSchemaMigrator applies the same statement on PostgreSQL.
-- Safe to re-run.

ALTER TABLE feedback_item
    ADD COLUMN IF NOT EXISTS author_reply_cleared_at TIMESTAMPTZ;
