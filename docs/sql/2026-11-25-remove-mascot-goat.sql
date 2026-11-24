-- Operator mirror of StudentStreakSchemaMigrator.
-- Remove retired exclusive mascot goat (and legacy gigi) and reset equipped_mascot to fox.
UPDATE student_daily_streak SET equipped_mascot = 'fox' WHERE equipped_mascot IN ('goat', 'gigi');
DELETE FROM student_mascot_purchase WHERE mascot_id IN ('goat', 'gigi');
