-- Operator-run: method-step repeat count for Composition (and Unit method loops).
-- Startup migrator applies the same ADD COLUMN on PostgreSQL.

ALTER TABLE testcase_invocation
    ADD COLUMN IF NOT EXISTS repeat_count INTEGER NOT NULL DEFAULT 1;

ALTER TABLE testcase_invocation ALTER COLUMN repeat_count DROP DEFAULT;
