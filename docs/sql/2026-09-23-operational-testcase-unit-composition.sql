-- Destructive operational-testcase rebuild: wipe OT graphs, drop COMPARISON /
-- oop_principle_tag / per-testcase weight, rewrite types to UNIT / COMPOSITION.
-- Operator-run. Idempotent enough to re-run on an already-wiped UNIT/COMPOSITION schema.
-- Does not append labels onto the old SINGLE_INVOCATION enum. Does not touch challenge.testcase_weight,
-- Class, or MMD tables. comparison_mode on assertions stays (EXACT / TRIMMED).

-- ---------- 1. Wipe OT result + rubric graphs when old labels or leftover columns remain ----------
DO $$
BEGIN
    IF EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typname = 'testcase_type'
              AND e.enumlabel IN ('SINGLE_INVOCATION', 'COMPARISON')
        )
        OR EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typname = 'assertion_kind'
              AND e.enumlabel = 'COMPARISON_RESULT'
        )
        OR EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'testcase'
              AND column_name IN ('oop_principle_tag', 'weight', 'comparison_method')
        )
        OR EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = 'testcase_instance'
        )
    THEN
        TRUNCATE TABLE submission_testcase_assertion_result CASCADE;
        TRUNCATE TABLE submission_testcase_result CASCADE;
        TRUNCATE TABLE testcase CASCADE;
    END IF;
END $$;

-- ---------- 2. Drop COMPARISON instance table and leftover testcase columns ----------
DROP TABLE IF EXISTS testcase_instance CASCADE;

ALTER TABLE testcase DROP CONSTRAINT IF EXISTS testcase_comparison_method_check;

ALTER TABLE testcase DROP COLUMN IF EXISTS comparison_method;
ALTER TABLE testcase DROP COLUMN IF EXISTS oop_principle_tag;
ALTER TABLE testcase DROP COLUMN IF EXISTS weight;

DROP TYPE IF EXISTS oop_principle_tag;
DROP TYPE IF EXISTS testcase_comparison_method;

-- ---------- 3. Rewrite testcase_type to UNIT / COMPOSITION (not ADD VALUE) ----------
DO $$
BEGIN
    IF EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typname = 'testcase_type'
              AND e.enumlabel IN ('SINGLE_INVOCATION', 'COMPARISON')
        )
        OR (
            EXISTS (SELECT 1 FROM pg_catalog.pg_type WHERE typname = 'testcase_type')
            AND NOT EXISTS (
                SELECT 1
                FROM pg_catalog.pg_type t
                JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
                WHERE t.typname = 'testcase_type'
                  AND e.enumlabel = 'UNIT'
            )
        )
    THEN
        IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_type WHERE typname = 'testcase_type_new') THEN
            CREATE TYPE testcase_type_new AS ENUM ('UNIT', 'COMPOSITION');
        END IF;
        ALTER TABLE testcase ALTER COLUMN testcase_type DROP DEFAULT;
        ALTER TABLE testcase
            ALTER COLUMN testcase_type TYPE testcase_type_new
            USING 'UNIT'::testcase_type_new;
        DROP TYPE testcase_type;
        ALTER TYPE testcase_type_new RENAME TO testcase_type;
    END IF;
END $$;

-- ---------- 4. Rewrite assertion_kind without COMPARISON_RESULT ----------
DO $$
BEGIN
    IF EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typname = 'assertion_kind'
              AND e.enumlabel = 'COMPARISON_RESULT'
        )
        OR EXISTS (SELECT 1 FROM pg_catalog.pg_type WHERE typname = 'assertion_kind_new')
    THEN
        ALTER TABLE testcase_assertion ALTER COLUMN assertion_kind DROP DEFAULT;
        ALTER TABLE testcase_assertion RENAME COLUMN assertion_kind TO assertion_kind_staging;
        ALTER TABLE testcase_assertion
            ADD COLUMN assertion_kind text NOT NULL DEFAULT 'RETURN_VALUE';
        UPDATE testcase_assertion
        SET assertion_kind = CASE
            WHEN assertion_kind_staging::text = 'COMPARISON_RESULT' THEN 'RETURN_VALUE'
            ELSE assertion_kind_staging::text
        END;
        ALTER TABLE testcase_assertion ALTER COLUMN assertion_kind DROP DEFAULT;
        ALTER TABLE testcase_assertion DROP COLUMN assertion_kind_staging;
        DROP TYPE IF EXISTS assertion_kind;
        DROP TYPE IF EXISTS assertion_kind_new;
        CREATE TYPE assertion_kind AS ENUM (
            'RETURN_VALUE', 'FIELD_STATE', 'STDOUT', 'EXCEPTION'
        );
        ALTER TABLE testcase_assertion
            ALTER COLUMN assertion_kind TYPE assertion_kind
            USING (assertion_kind::text::assertion_kind);
    ELSIF EXISTS (SELECT 1 FROM pg_catalog.pg_type WHERE typname = 'assertion_kind_new') THEN
        DROP TYPE assertion_kind_new;
    END IF;
END $$;
