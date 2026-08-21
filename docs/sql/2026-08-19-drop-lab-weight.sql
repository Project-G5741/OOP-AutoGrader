-- Remove lab-level scoring weight. Operator-run. Safe to re-run.

ALTER TABLE lab DROP CONSTRAINT IF EXISTS lab_weight_positive;
ALTER TABLE lab DROP COLUMN IF EXISTS weight;
