-- Operator-run: remember yes/no for spending points on gacha (Vietnam calendar day).
ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS gacha_points_pref_date DATE;
ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS gacha_points_pref_allow BOOLEAN;