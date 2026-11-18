-- Operator mirror of StudentStreakSchemaMigrator.
-- Rename the exclusive mascot id gigi to goat.
UPDATE student_daily_streak SET equipped_mascot = 'goat' WHERE equipped_mascot = 'gigi';
UPDATE student_mascot_purchase SET mascot_id = 'goat' WHERE mascot_id = 'gigi';
