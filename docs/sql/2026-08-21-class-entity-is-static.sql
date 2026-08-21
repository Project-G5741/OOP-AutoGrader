-- Static nested vs non-static inner flag for rubric nested classes.
-- Operator-run. Safe to re-run: IF NOT EXISTS.
-- Meaningful only when outer_class_id is set; top-level rows stay false.

ALTER TABLE class_entity
    ADD COLUMN IF NOT EXISTS is_static BOOLEAN NOT NULL DEFAULT false;
