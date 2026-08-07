-- Operator-run script: creates term_enrollment and seeds from existing lab progress.
-- Run against the project PostgreSQL database before using lecturer roster APIs.
-- The backend also auto-syncs on startup (TermEnrollmentSyncService) and roster queries
-- include students with student_lab_progress even when enrollment rows are missing.

CREATE TABLE IF NOT EXISTS term_enrollment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id),
    term_id UUID NOT NULL REFERENCES term(id),
    CONSTRAINT term_enrollment_user_term_key UNIQUE (user_id, term_id)
);

INSERT INTO term_enrollment (id, user_id, term_id)
SELECT gen_random_uuid(), p.user_id, l.term_id
FROM student_lab_progress p
JOIN lab l ON l.id = p.lab_id
ON CONFLICT ON CONSTRAINT term_enrollment_user_term_key DO NOTHING;

-- Enroll additional students manually, e.g.:
-- INSERT INTO term_enrollment (id, user_id, term_id)
-- VALUES (gen_random_uuid(), '<student-uuid>', '<term-uuid>')
-- ON CONFLICT ON CONSTRAINT term_enrollment_user_term_key DO NOTHING;
