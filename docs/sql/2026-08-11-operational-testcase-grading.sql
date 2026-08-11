-- Operator-run migration: replace EXISTENCE/DECLARATION testcases with operational invoke-based testcases.
-- Destructive: wipes structural testcase data. Review before applying to production.

-- ---------- 1. Drop obsolete structural data ----------
TRUNCATE submission_testcase_result CASCADE;
TRUNCATE testcase CASCADE;

DROP TRIGGER IF EXISTS trg_validate_testcase_target ON testcase;

ALTER TABLE testcase DROP COLUMN IF EXISTS check_type;
ALTER TABLE testcase DROP COLUMN IF EXISTS target_type;
ALTER TABLE testcase DROP COLUMN IF EXISTS target_id;

DROP TYPE IF EXISTS testcase_check_type;
DROP TYPE IF EXISTS testcase_target_type;

-- ---------- 2. New enums ----------
DO $$ BEGIN
    CREATE TYPE testcase_type AS ENUM ('SINGLE_INVOCATION', 'COMPARISON');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE invocation_kind AS ENUM ('CONSTRUCTOR', 'METHOD');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE assertion_kind AS ENUM (
        'RETURN_VALUE', 'FIELD_STATE', 'STDOUT', 'EXCEPTION', 'COMPARISON_RESULT'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE comparison_mode AS ENUM ('EXACT', 'TRIMMED', 'NORMALIZED_WHITESPACE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE testcase_comparison_method AS ENUM ('EQUALS', 'COMPARE_TO');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ---------- 3. testcase (updated) ----------
ALTER TABLE testcase ADD COLUMN IF NOT EXISTS testcase_type testcase_type NOT NULL DEFAULT 'SINGLE_INVOCATION';
ALTER TABLE testcase ALTER COLUMN testcase_type DROP DEFAULT;
ALTER TABLE testcase ADD COLUMN IF NOT EXISTS comparison_method testcase_comparison_method;

ALTER TABLE testcase DROP CONSTRAINT IF EXISTS testcase_comparison_method_check;
ALTER TABLE testcase ADD CONSTRAINT testcase_comparison_method_check
    CHECK (
        (testcase_type = 'COMPARISON' AND comparison_method IS NOT NULL)
        OR (testcase_type != 'COMPARISON' AND comparison_method IS NULL)
    );

-- ---------- 4. submission_testcase_result display columns ----------
ALTER TABLE submission_testcase_result ADD COLUMN IF NOT EXISTS input_display TEXT;
ALTER TABLE submission_testcase_result ADD COLUMN IF NOT EXISTS expected_display TEXT;
ALTER TABLE submission_testcase_result ADD COLUMN IF NOT EXISTS actual_display TEXT;

-- ---------- 5. testcase_invocation (no stdin in v1) ----------
CREATE TABLE IF NOT EXISTS testcase_invocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    testcase_id UUID NOT NULL,
    invocation_kind invocation_kind NOT NULL,
    constructor_id UUID,
    method_id UUID,
    params JSONB NOT NULL DEFAULT '[]',
    CONSTRAINT testcase_invocation_testcase_id_key UNIQUE (testcase_id),
    CONSTRAINT testcase_invocation_kind_check CHECK (
        (invocation_kind = 'CONSTRUCTOR' AND constructor_id IS NOT NULL AND method_id IS NULL)
        OR (invocation_kind = 'METHOD' AND method_id IS NOT NULL AND constructor_id IS NULL)
    )
);

ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_testcase_id_fkey;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_testcase_id_fkey
    FOREIGN KEY (testcase_id) REFERENCES testcase(id) ON DELETE CASCADE;
ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_constructor_id_fkey;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_constructor_id_fkey
    FOREIGN KEY (constructor_id) REFERENCES constructor(id) ON DELETE CASCADE;
ALTER TABLE testcase_invocation DROP CONSTRAINT IF EXISTS testcase_invocation_method_id_fkey;
ALTER TABLE testcase_invocation ADD CONSTRAINT testcase_invocation_method_id_fkey
    FOREIGN KEY (method_id) REFERENCES method(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_testcase_invocation_testcase_id ON testcase_invocation(testcase_id);

-- ---------- 6. testcase_instance ----------
CREATE TABLE IF NOT EXISTS testcase_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    testcase_id UUID NOT NULL,
    label TEXT NOT NULL,
    constructor_id UUID NOT NULL,
    params JSONB NOT NULL DEFAULT '[]',
    CONSTRAINT testcase_instance_label_key UNIQUE (testcase_id, label)
);

ALTER TABLE testcase_instance DROP CONSTRAINT IF EXISTS testcase_instance_testcase_id_fkey;
ALTER TABLE testcase_instance ADD CONSTRAINT testcase_instance_testcase_id_fkey
    FOREIGN KEY (testcase_id) REFERENCES testcase(id) ON DELETE CASCADE;
ALTER TABLE testcase_instance DROP CONSTRAINT IF EXISTS testcase_instance_constructor_id_fkey;
ALTER TABLE testcase_instance ADD CONSTRAINT testcase_instance_constructor_id_fkey
    FOREIGN KEY (constructor_id) REFERENCES constructor(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_testcase_instance_testcase_id ON testcase_instance(testcase_id);

-- ---------- 7. testcase_assertion ----------
CREATE TABLE IF NOT EXISTS testcase_assertion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    testcase_id UUID NOT NULL,
    invocation_id UUID,
    assertion_kind assertion_kind NOT NULL,
    field_id UUID,
    expected_value JSONB NOT NULL,
    comparison_mode comparison_mode NOT NULL DEFAULT 'EXACT',
    order_index INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT testcase_assertion_field_check CHECK (
        (assertion_kind = 'FIELD_STATE' AND field_id IS NOT NULL)
        OR (assertion_kind != 'FIELD_STATE' AND field_id IS NULL)
    )
);

ALTER TABLE testcase_assertion DROP CONSTRAINT IF EXISTS testcase_assertion_testcase_id_fkey;
ALTER TABLE testcase_assertion ADD CONSTRAINT testcase_assertion_testcase_id_fkey
    FOREIGN KEY (testcase_id) REFERENCES testcase(id) ON DELETE CASCADE;
ALTER TABLE testcase_assertion DROP CONSTRAINT IF EXISTS testcase_assertion_invocation_id_fkey;
ALTER TABLE testcase_assertion ADD CONSTRAINT testcase_assertion_invocation_id_fkey
    FOREIGN KEY (invocation_id) REFERENCES testcase_invocation(id) ON DELETE CASCADE;
ALTER TABLE testcase_assertion DROP CONSTRAINT IF EXISTS testcase_assertion_field_id_fkey;
ALTER TABLE testcase_assertion ADD CONSTRAINT testcase_assertion_field_id_fkey
    FOREIGN KEY (field_id) REFERENCES field(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_testcase_assertion_testcase_id ON testcase_assertion(testcase_id);

-- ---------- 8. submission_testcase_assertion_result ----------
CREATE TABLE IF NOT EXISTS submission_testcase_assertion_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_testcase_result_id UUID NOT NULL,
    testcase_assertion_id UUID NOT NULL,
    result testcase_result_status NOT NULL,
    actual_value JSONB,
    feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT submission_testcase_assertion_result_key
        UNIQUE (submission_testcase_result_id, testcase_assertion_id)
);

ALTER TABLE submission_testcase_assertion_result DROP CONSTRAINT IF EXISTS staresult_str_id_fkey;
ALTER TABLE submission_testcase_assertion_result ADD CONSTRAINT staresult_str_id_fkey
    FOREIGN KEY (submission_testcase_result_id) REFERENCES submission_testcase_result(id) ON DELETE CASCADE;
ALTER TABLE submission_testcase_assertion_result DROP CONSTRAINT IF EXISTS staresult_assertion_id_fkey;
ALTER TABLE submission_testcase_assertion_result ADD CONSTRAINT staresult_assertion_id_fkey
    FOREIGN KEY (testcase_assertion_id) REFERENCES testcase_assertion(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_staresult_str_id ON submission_testcase_assertion_result(submission_testcase_result_id);
