-- Operator-run: durable student notification read-receipt keys (web bell).
-- Startup migrator StudentNotificationReadSchemaMigrator applies the same statement on PostgreSQL.

CREATE TABLE IF NOT EXISTS student_notification_read (
    user_id    UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    notif_key  VARCHAR(191) NOT NULL,
    read_at    TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, notif_key)
);
