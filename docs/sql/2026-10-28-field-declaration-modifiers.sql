-- Operator-run: field_declaration static/final flags for Declaration Test rubric.
ALTER TABLE field_declaration
    ADD COLUMN IF NOT EXISTS is_static BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE field_declaration
    ADD COLUMN IF NOT EXISTS is_final BOOLEAN NOT NULL DEFAULT false;
