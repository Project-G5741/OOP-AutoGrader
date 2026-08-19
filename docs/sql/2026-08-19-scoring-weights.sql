-- Scoring weights for challenge, class, and MMD pillar.
-- Operator-run. Safe to re-run: IF NOT EXISTS / default 1.
-- Labs have no weight (see 2026-08-19-drop-lab-weight.sql).

ALTER TABLE challenge
    ADD COLUMN IF NOT EXISTS weight INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS class_weight INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS mmd_weight INTEGER NOT NULL DEFAULT 1;

ALTER TABLE class_entity
    ADD COLUMN IF NOT EXISTS weight INTEGER NOT NULL DEFAULT 1;

ALTER TABLE challenge DROP CONSTRAINT IF EXISTS challenge_weight_positive;
ALTER TABLE challenge ADD CONSTRAINT challenge_weight_positive CHECK (weight >= 1);
ALTER TABLE challenge DROP CONSTRAINT IF EXISTS challenge_class_weight_positive;
ALTER TABLE challenge ADD CONSTRAINT challenge_class_weight_positive CHECK (class_weight >= 1);
ALTER TABLE challenge DROP CONSTRAINT IF EXISTS challenge_mmd_weight_positive;
ALTER TABLE challenge ADD CONSTRAINT challenge_mmd_weight_positive CHECK (mmd_weight >= 1);

ALTER TABLE class_entity DROP CONSTRAINT IF EXISTS class_entity_weight_positive;
ALTER TABLE class_entity ADD CONSTRAINT class_entity_weight_positive CHECK (weight >= 1);
