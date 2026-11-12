-- Operator-run: pillar percentages for the highest-scoring attempt display.
-- Startup migrator applies the same ADD COLUMN IF NOT EXISTS on PostgreSQL.
-- Null means the pillar does not apply (new grades) or the attempt predates this column.

ALTER TABLE lab_submission
    ADD COLUMN IF NOT EXISTS mmd_score NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS declaration_score NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS operational_score NUMERIC(6, 2);

ALTER TABLE submission_challenge_result
    ADD COLUMN IF NOT EXISTS mmd_score NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS declaration_score NUMERIC(6, 2),
    ADD COLUMN IF NOT EXISTS operational_score NUMERIC(6, 2);
