-- Remove unused PII column date_of_birth from user_account. Operator-run. Safe to re-run.

ALTER TABLE user_account DROP COLUMN IF EXISTS date_of_birth;
