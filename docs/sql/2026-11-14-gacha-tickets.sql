-- Gacha ticket balance on the student streak row.
-- Operator-run. Safe to re-run. Startup migrator applies the same columns on PostgreSQL.
-- The 100-ticket launch gift is a separate one-time UPDATE, not part of this file,
-- so a restart does not grant another 100.

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS gacha_tickets INTEGER NOT NULL DEFAULT 0;

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS mission_tickets_awarded INTEGER NOT NULL DEFAULT 0;

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS gacha_launch_gift BOOLEAN NOT NULL DEFAULT FALSE;
