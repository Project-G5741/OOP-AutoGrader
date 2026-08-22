-- Operational testcase pillar weight on challenge.
-- Operator-run. Safe to re-run: IF NOT EXISTS / default 1.

ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS testcase_weight INTEGER NOT NULL DEFAULT 1;

ALTER TABLE challenge DROP CONSTRAINT IF EXISTS challenge_testcase_weight_positive;
ALTER TABLE challenge ADD CONSTRAINT challenge_testcase_weight_positive CHECK (testcase_weight >= 1);
