-- Additive operational-testcase scenario columns: principle tag, ordered invocation
-- steps, named instances, and optional dispatch class. Safe to re-run.
-- Does not truncate testcase data. COMPARISON tables are unchanged.

DO $$ BEGIN
    CREATE TYPE oop_principle_tag AS ENUM (
        'Unit',
        'Polymorphism',
        'Encapsulation',
        'Composition',
        'Inheritance'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

ALTER TABLE testcase
    ADD COLUMN IF NOT EXISTS oop_principle_tag oop_principle_tag NOT NULL DEFAULT 'Unit';

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS order_index INTEGER NOT NULL DEFAULT 0;

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS instance_name TEXT NULL;

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS dispatch_class_id UUID NULL;

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_dispatch_class_id_fkey;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_dispatch_class_id_fkey
    FOREIGN KEY (dispatch_class_id) REFERENCES class_entity(id) ON DELETE CASCADE;

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_testcase_id_key;
ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_testcase_id_order_index_key;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_testcase_id_order_index_key
    UNIQUE (testcase_id, order_index);
