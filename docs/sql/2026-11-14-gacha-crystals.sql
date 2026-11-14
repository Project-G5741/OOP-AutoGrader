-- Crystal balance on the student streak row.
-- Operator-run. Safe to re-run. Startup migrator applies the same column on PostgreSQL.
-- Students exchange crystals for reward points at 4000 points each.

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS crystals INTEGER NOT NULL DEFAULT 0;
