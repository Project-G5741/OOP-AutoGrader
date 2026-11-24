-- Operator mirror: student_daily_streak.egg_smash_pity (0..8 hidden pity counter for Crystal Egg Smash).
-- Startup: EggSmashSchemaMigrator on PostgreSQL.

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS egg_smash_pity INTEGER NOT NULL DEFAULT 0;
