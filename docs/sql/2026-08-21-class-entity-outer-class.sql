-- Optional outer-class link for nested rubric classes (one level: Outer.Inner).
-- Operator-run. Safe to re-run: IF NOT EXISTS.

ALTER TABLE class_entity
    ADD COLUMN IF NOT EXISTS outer_class_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'class_entity_outer_class_id_fkey'
    ) THEN
        ALTER TABLE class_entity
            ADD CONSTRAINT class_entity_outer_class_id_fkey
            FOREIGN KEY (outer_class_id) REFERENCES class_entity(id) ON DELETE CASCADE;
    END IF;
END $$;
