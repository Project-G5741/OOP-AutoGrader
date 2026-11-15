-- Operator-run: durable student notification dismiss keys (web bell forever-hide).
-- Startup migrator StudentNotificationDismissSchemaMigrator applies the same statement on PostgreSQL.

CREATE TABLE IF NOT EXISTS student_notification_dismiss (
    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    notif_key    VARCHAR(191) NOT NULL,
    dismissed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, notif_key)
);
