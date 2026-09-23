-- Destructive operational-testcase rebuild: wipe OT graphs, drop COMPARISON /
-- oop_principle_tag / per-testcase weight, rewrite types to UNIT / COMPOSITION.
-- Operator-run. Idempotent enough to re-run on an already-wiped UNIT/COMPOSITION schema.
-- Does not append labels onto the old SINGLE_INVOCATION enum. Does not touch challenge.testcase_weight,
-- Class, or MMD tables. comparison_mode on assertions stays (EXACT / TRIMMED).
-- Canonical copy for operators: docs/sql/2026-09-23-operational-testcase-unit-composition.sql

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

DROP TABLE IF EXISTS testcase_instance CASCADE;

ALTER TABLE testcase DROP CONSTRAINT IF EXISTS testcase_comparison_method_check;

ALTER TABLE testcase DROP COLUMN IF EXISTS comparison_method;
ALTER TABLE testcase DROP COLUMN IF EXISTS oop_principle_tag;
ALTER TABLE testcase DROP COLUMN IF EXISTS weight;

DROP TYPE IF EXISTS oop_principle_tag;
DROP TYPE IF EXISTS testcase_comparison_method;

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

DO $$ BEGIN
    CREATE TYPE assertion_kind_new AS ENUM (
        'RETURN_VALUE', 'FIELD_STATE', 'STDOUT', 'EXCEPTION'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    IF EXISTS (
            SELECT 1
            FROM pg_catalog.pg_type t
            JOIN pg_catalog.pg_enum e ON e.enumtypid = t.oid
            WHERE t.typname = 'assertion_kind'
              AND e.enumlabel = 'COMPARISON_RESULT'
        )
    THEN
        ALTER TABLE testcase_assertion ALTER COLUMN assertion_kind DROP DEFAULT;
        ALTER TABLE testcase_assertion
            ALTER COLUMN assertion_kind TYPE assertion_kind_new
            USING (
                CASE
                    WHEN assertion_kind::text = 'COMPARISON_RESULT' THEN 'RETURN_VALUE'
                    ELSE assertion_kind::text
                END
            )::assertion_kind_new;
        DROP TYPE assertion_kind;
        ALTER TYPE assertion_kind_new RENAME TO assertion_kind;
    ELSIF EXISTS (SELECT 1 FROM pg_catalog.pg_type WHERE typname = 'assertion_kind_new') THEN
        DROP TYPE assertion_kind_new;
    END IF;
END $$;
