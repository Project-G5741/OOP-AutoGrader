-- Operator-run migration: structural testcase grading tables.
-- Idempotent where practical; review before applying to production.

-- Gap 1: master_data grouping for combo-box lookups
ALTER TABLE master_data ADD COLUMN IF NOT EXISTS category TEXT NOT NULL DEFAULT 'UNSPECIFIED';
CREATE INDEX IF NOT EXISTS idx_master_data_category ON master_data(category);

-- Gap 2 + testcase tables
DO $$ BEGIN
    CREATE TYPE testcase_check_type AS ENUM ('EXISTENCE', 'DECLARATION');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE testcase_target_type AS ENUM ('CLASS', 'FIELD', 'METHOD', 'CONSTRUCTOR');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE testcase_result_status AS ENUM ('PASSED', 'FAILED', 'ERROR', 'SKIPPED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS testcase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID NOT NULL REFERENCES challenge(id) ON DELETE CASCADE,
    check_type testcase_check_type NOT NULL,
    target_type testcase_target_type NOT NULL,
    target_id UUID NOT NULL,
    name TEXT NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_testcase_challenge_id ON testcase(challenge_id);
CREATE INDEX IF NOT EXISTS idx_testcase_target ON testcase(target_type, target_id);

CREATE OR REPLACE FUNCTION validate_testcase_target() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.target_type = 'CLASS' AND NOT EXISTS (SELECT 1 FROM class_entity WHERE id = NEW.target_id) THEN
        RAISE EXCEPTION 'target_id % not found in class_entity', NEW.target_id;
    ELSIF NEW.target_type = 'FIELD' AND NOT EXISTS (SELECT 1 FROM field WHERE id = NEW.target_id) THEN
        RAISE EXCEPTION 'target_id % not found in field', NEW.target_id;
    ELSIF NEW.target_type = 'METHOD' AND NOT EXISTS (SELECT 1 FROM method WHERE id = NEW.target_id) THEN
        RAISE EXCEPTION 'target_id % not found in method', NEW.target_id;
    ELSIF NEW.target_type = 'CONSTRUCTOR' AND NOT EXISTS (SELECT 1 FROM constructor WHERE id = NEW.target_id) THEN
        RAISE EXCEPTION 'target_id % not found in constructor', NEW.target_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_testcase_target ON testcase;
CREATE TRIGGER trg_validate_testcase_target
    BEFORE INSERT OR UPDATE ON testcase
    FOR EACH ROW EXECUTE FUNCTION validate_testcase_target();

CREATE TABLE IF NOT EXISTS submission_testcase_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    testcase_id UUID NOT NULL REFERENCES testcase(id) ON DELETE CASCADE,
    result testcase_result_status NOT NULL,
    feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (submission_id, testcase_id)
);

CREATE INDEX IF NOT EXISTS idx_str_submission_id ON submission_testcase_result(submission_id);
CREATE INDEX IF NOT EXISTS idx_str_testcase_id ON submission_testcase_result(testcase_id);
