-- Operator mirror of StudentStreakSchemaMigrator mascot id canonicalization and equipped backfill.
-- Normalizes hyphenated/mixed-case mascot ids and backfills any currently equipped non-starter mascot
-- missing a row in student_mascot_purchase. Safe to re-run.

UPDATE student_daily_streak
SET equipped_mascot = LOWER(REPLACE(TRIM(equipped_mascot), '-', '_'))
WHERE equipped_mascot IS NOT NULL
  AND equipped_mascot <> LOWER(REPLACE(TRIM(equipped_mascot), '-', '_'));

INSERT INTO student_mascot_purchase (user_id, mascot_id, price_paid, purchased_at)
SELECT user_id, LOWER(REPLACE(TRIM(mascot_id), '-', '_')), price_paid, purchased_at
FROM student_mascot_purchase
WHERE mascot_id <> LOWER(REPLACE(TRIM(mascot_id), '-', '_'))
ON CONFLICT (user_id, mascot_id) DO NOTHING;

DELETE FROM student_mascot_purchase
WHERE mascot_id <> LOWER(REPLACE(TRIM(mascot_id), '-', '_'));

INSERT INTO student_mascot_purchase (user_id, mascot_id, price_paid, purchased_at)
SELECT s.user_id, s.equipped_mascot, 0, NOW()
FROM student_daily_streak s
WHERE s.equipped_mascot IS NOT NULL
  AND s.equipped_mascot NOT IN ('', 'fox', 'goat', 'gigi')
ON CONFLICT (user_id, mascot_id) DO NOTHING;
