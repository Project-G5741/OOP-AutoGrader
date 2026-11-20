-- Operator-run: crystal gacha pity counter per student (0–80).
ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS gacha_pity_count INTEGER NOT NULL DEFAULT 0;