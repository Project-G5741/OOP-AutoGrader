-- Align Neon unique constraint name with JPA / JDBC upsert target.
-- Live DB had Postgres-default name submission_testcase_result_submission_id_testcase_id_key;
-- entity + GradingResultJdbcWriter expect submission_testcase_result_key.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'submission_testcase_result_submission_id_testcase_id_key'
          AND conrelid = 'submission_testcase_result'::regclass
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'submission_testcase_result_key'
          AND conrelid = 'submission_testcase_result'::regclass
    ) THEN
        ALTER TABLE submission_testcase_result
            RENAME CONSTRAINT submission_testcase_result_submission_id_testcase_id_key
            TO submission_testcase_result_key;
    END IF;
END $$;
