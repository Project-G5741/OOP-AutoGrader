-- OOP AutoGrader — reconstructed PostgreSQL schema baseline (empty DB).
--
-- Source of truth when Neon was unreachable (quota exceeded):
--   - JPA @Entity classes under backend/src/main/java/.../model/
--   - Operator SQL under docs/sql/ (post 2026-09-23 UNIT/COMPOSITION OT shape)
--   - Startup migrators (session_version, desktop_pack_version, OT wipe, weights)
--
-- NOT a live pg_dump. Types/FK ON DELETE for older rubric tables are best-effort.
-- Apply to an empty database, then re-seed roles/master_data/labs as needed.
-- JPA maps several OT enums as VARCHAR; keep native ENUM types here and
-- spring.datasource.hikari.data-source-properties.stringtype=unspecified.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ========== ENUMS (current) ==========
DO $$ BEGIN
    CREATE TYPE testcase_type AS ENUM ('UNIT', 'COMPOSITION');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE invocation_kind AS ENUM ('CONSTRUCTOR', 'METHOD');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE assertion_kind AS ENUM (
        'RETURN_VALUE', 'FIELD_STATE', 'STDOUT', 'EXCEPTION'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE comparison_mode AS ENUM (
        'EXACT', 'TRIMMED', 'NORMALIZED_WHITESPACE', 'VALUE_ONLY'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE testcase_result_status AS ENUM (
        'PASSED', 'FAILED', 'ERROR', 'SKIPPED'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ========== AUTH / USERS ==========
CREATE TABLE IF NOT EXISTS role (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_account (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_code    TEXT UNIQUE,
    teacher_code    TEXT UNIQUE,
    full_name       TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    session_version INTEGER NOT NULL DEFAULT 0,
    create_at       TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id
    ON password_reset_token(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires_at
    ON password_reset_token(expires_at);

-- ========== TERMS ==========
CREATE TABLE IF NOT EXISTS academic_years (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year_label TEXT NOT NULL UNIQUE,
    start_date DATE,
    end_date   DATE
);

CREATE TABLE IF NOT EXISTS term (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id),
    term_number      INTEGER NOT NULL,
    start_date       DATE,
    end_date         DATE,
    is_current       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_term_is_current
    ON term (is_current) WHERE is_current;

CREATE TABLE IF NOT EXISTS term_enrollment (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES user_account(id),
    term_id UUID NOT NULL REFERENCES term(id),
    CONSTRAINT term_enrollment_user_term_key UNIQUE (user_id, term_id)
);

CREATE INDEX IF NOT EXISTS idx_term_enrollment_term_id
    ON term_enrollment (term_id);

-- ========== LABS / CHALLENGES ==========
CREATE TABLE IF NOT EXISTS lab (
    id              UUID PRIMARY KEY,
    name            TEXT NOT NULL,
    term_id         UUID NOT NULL REFERENCES term(id),
    deadline_date   DATE,
    student_visible BOOLEAN NOT NULL DEFAULT TRUE,
    release_date    DATE
);

CREATE TABLE IF NOT EXISTS challenge (
    id               UUID PRIMARY KEY,
    name             TEXT NOT NULL,
    lab_id           UUID NOT NULL REFERENCES lab(id),
    challenge_number INTEGER NOT NULL,
    has_mmd          BOOLEAN NOT NULL DEFAULT TRUE,
    has_class        BOOLEAN NOT NULL DEFAULT TRUE,
    weight           INTEGER NOT NULL DEFAULT 1,
    class_weight     INTEGER NOT NULL DEFAULT 1,
    mmd_weight       INTEGER NOT NULL DEFAULT 1,
    testcase_weight  INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT challenge_lab_id_challenge_number_key UNIQUE (lab_id, challenge_number),
    CONSTRAINT challenge_weight_positive CHECK (weight >= 1),
    CONSTRAINT challenge_class_weight_positive CHECK (class_weight >= 1),
    CONSTRAINT challenge_mmd_weight_positive CHECK (mmd_weight >= 1),
    CONSTRAINT challenge_testcase_weight_positive CHECK (testcase_weight >= 1)
);

CREATE TABLE IF NOT EXISTS lab_deadline_email_sent (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id          UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    threshold_hours SMALLINT NOT NULL CHECK (threshold_hours IN (72, 24)),
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (lab_id, user_id, threshold_hours)
);

CREATE INDEX IF NOT EXISTS idx_lab_deadline_email_sent_lab_threshold
    ON lab_deadline_email_sent (lab_id, threshold_hours);

-- ========== MASTER DATA / RUBRIC STRUCTURE ==========
CREATE TABLE IF NOT EXISTS master_data (
    id       INTEGER PRIMARY KEY,
    name     TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'UNSPECIFIED'
);

CREATE INDEX IF NOT EXISTS idx_master_data_category ON master_data(category);

CREATE TABLE IF NOT EXISTS class_entity (
    id               UUID PRIMARY KEY,
    name             TEXT NOT NULL,
    challenge_id     UUID NOT NULL REFERENCES challenge(id),
    scope            INTEGER NOT NULL REFERENCES master_data(id),
    declaring_type   INTEGER NOT NULL REFERENCES master_data(id),
    outer_class_id   UUID REFERENCES class_entity(id) ON DELETE CASCADE,
    is_abstract      BOOLEAN NOT NULL DEFAULT FALSE,
    is_static        BOOLEAN NOT NULL DEFAULT FALSE,
    weight           INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT class_entity_weight_positive CHECK (weight >= 1)
);

CREATE TABLE IF NOT EXISTS field_declaration (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      TEXT NOT NULL,
    data_type TEXT,
    scope     INTEGER REFERENCES master_data(id)
);

CREATE TABLE IF NOT EXISTS method_declaration (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    scope       INTEGER NOT NULL REFERENCES master_data(id),
    return_type TEXT NOT NULL,
    is_static   BOOLEAN NOT NULL DEFAULT FALSE,
    is_abstract BOOLEAN NOT NULL DEFAULT FALSE,
    is_final    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS constructor_declaration (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    scope      INTEGER NOT NULL REFERENCES master_data(id),
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS field (
    id                   UUID PRIMARY KEY,
    class_id             UUID NOT NULL REFERENCES class_entity(id),
    field_declaration_id UUID NOT NULL REFERENCES field_declaration(id),
    name                 TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS method (
    id                    UUID PRIMARY KEY,
    class_id              UUID NOT NULL REFERENCES class_entity(id),
    method_declaration_id UUID NOT NULL REFERENCES method_declaration(id),
    name                  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS constructor (
    id                         UUID PRIMARY KEY,
    class_id                   UUID NOT NULL REFERENCES class_entity(id),
    constructor_declaration_id UUID NOT NULL REFERENCES constructor_declaration(id),
    name                       TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS parameter (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_id      UUID REFERENCES method(id),
    constructor_id UUID REFERENCES constructor(id),
    name           TEXT NOT NULL,
    data_type      TEXT NOT NULL,
    order_index    INTEGER NOT NULL,
    is_final       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS class_relation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id        UUID NOT NULL REFERENCES class_entity(id),
    target_class_id UUID NOT NULL REFERENCES class_entity(id),
    relation_type   INTEGER NOT NULL REFERENCES master_data(id)
);

-- ========== OPERATIONAL TESTCASES (UNIT / COMPOSITION) ==========
CREATE TABLE IF NOT EXISTS testcase (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id  UUID NOT NULL REFERENCES challenge(id) ON DELETE CASCADE,
    testcase_type testcase_type NOT NULL,
    name          TEXT NOT NULL,
    order_index   INTEGER NOT NULL DEFAULT 0,
    is_hidden     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_testcase_challenge_id ON testcase(challenge_id);

CREATE TABLE IF NOT EXISTS testcase_invocation (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    testcase_id             UUID NOT NULL REFERENCES testcase(id) ON DELETE CASCADE,
    invocation_kind         invocation_kind NOT NULL,
    constructor_id          UUID REFERENCES constructor(id) ON DELETE CASCADE,
    method_id               UUID REFERENCES method(id) ON DELETE CASCADE,
    params                  JSONB NOT NULL DEFAULT '[]',
    receiver_constructor_id UUID REFERENCES constructor(id) ON DELETE CASCADE,
    receiver_params         JSONB NOT NULL DEFAULT '[]',
    order_index             INTEGER NOT NULL DEFAULT 0,
    instance_name           TEXT,
    dispatch_class_id       UUID REFERENCES class_entity(id) ON DELETE CASCADE,
    CONSTRAINT testcase_invocation_testcase_id_order_index_key
        UNIQUE (testcase_id, order_index),
    CONSTRAINT testcase_invocation_kind_check CHECK (
        (invocation_kind = 'CONSTRUCTOR' AND constructor_id IS NOT NULL AND method_id IS NULL)
        OR (invocation_kind = 'METHOD' AND method_id IS NOT NULL AND constructor_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_testcase_invocation_testcase_id
    ON testcase_invocation(testcase_id);

CREATE TABLE IF NOT EXISTS testcase_assertion (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    testcase_id      UUID NOT NULL REFERENCES testcase(id) ON DELETE CASCADE,
    invocation_id    UUID REFERENCES testcase_invocation(id) ON DELETE CASCADE,
    assertion_kind   assertion_kind NOT NULL,
    field_id         UUID REFERENCES field(id) ON DELETE CASCADE,
    expected_value   JSONB NOT NULL,
    comparison_mode  comparison_mode NOT NULL DEFAULT 'EXACT',
    order_index      INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT testcase_assertion_field_check CHECK (
        (assertion_kind = 'FIELD_STATE' AND field_id IS NOT NULL)
        OR (assertion_kind != 'FIELD_STATE' AND field_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_testcase_assertion_testcase_id
    ON testcase_assertion(testcase_id);

-- ========== SUBMISSIONS / PROGRESS ==========
CREATE TABLE IF NOT EXISTS lab_submission (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES user_account(id),
    lab_id               UUID NOT NULL REFERENCES lab(id),
    attempt_number       INTEGER NOT NULL,
    score                NUMERIC(6, 2) NOT NULL DEFAULT 0,
    submitted_at         TIMESTAMPTZ NOT NULL,
    desktop_pack_version VARCHAR(255),
    CONSTRAINT lab_submission_user_lab_attempt_key
        UNIQUE (user_id, lab_id, attempt_number)
);

CREATE INDEX IF NOT EXISTS idx_lab_submission_submitted_at_desc
    ON lab_submission (submitted_at DESC);

CREATE TABLE IF NOT EXISTS student_lab_progress (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES user_account(id),
    lab_id             UUID NOT NULL REFERENCES lab(id),
    highest_score      NUMERIC(6, 2) NOT NULL DEFAULT 0,
    attempts_count     INTEGER NOT NULL DEFAULT 0,
    best_submission_id UUID,
    first_submitted_at TIMESTAMPTZ,
    last_submitted_at  TIMESTAMPTZ,
    CONSTRAINT student_lab_progress_user_lab_key UNIQUE (user_id, lab_id)
);

CREATE INDEX IF NOT EXISTS idx_slp_active_submitters
    ON student_lab_progress (user_id)
    WHERE last_submitted_at IS NOT NULL;

-- ========== SUBMISSION RESULTS ==========
CREATE TABLE IF NOT EXISTS submission_challenge_result (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES lab_submission(id),
    challenge_id  UUID NOT NULL REFERENCES challenge(id),
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE,
    score         NUMERIC(6, 2) NOT NULL DEFAULT 0,
    mmd_outcomes  JSONB,
    CONSTRAINT submission_challenge_result_key UNIQUE (submission_id, challenge_id)
);

CREATE TABLE IF NOT EXISTS submission_field_result (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES lab_submission(id),
    field_id      UUID NOT NULL REFERENCES field(id),
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT submission_field_result_key UNIQUE (submission_id, field_id)
);

CREATE TABLE IF NOT EXISTS submission_method_result (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES lab_submission(id),
    method_id     UUID NOT NULL REFERENCES method(id),
    is_correct    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT submission_method_result_key UNIQUE (submission_id, method_id)
);

CREATE TABLE IF NOT EXISTS submission_constructor_result (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id  UUID NOT NULL REFERENCES lab_submission(id),
    constructor_id UUID NOT NULL REFERENCES constructor(id),
    is_correct     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT submission_constructor_result_key UNIQUE (submission_id, constructor_id)
);

CREATE TABLE IF NOT EXISTS submission_relation_result (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id     UUID NOT NULL REFERENCES lab_submission(id),
    class_relation_id UUID NOT NULL REFERENCES class_relation(id),
    is_correct        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT submission_relation_result_key UNIQUE (submission_id, class_relation_id)
);

CREATE TABLE IF NOT EXISTS submission_testcase_result (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id    UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    testcase_id      UUID NOT NULL REFERENCES testcase(id) ON DELETE CASCADE,
    result           testcase_result_status NOT NULL,
    feedback         TEXT,
    input_display    TEXT,
    expected_display TEXT,
    actual_display   TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT submission_testcase_result_key UNIQUE (submission_id, testcase_id)
);

CREATE INDEX IF NOT EXISTS idx_str_submission_id ON submission_testcase_result(submission_id);
CREATE INDEX IF NOT EXISTS idx_str_testcase_id ON submission_testcase_result(testcase_id);

CREATE TABLE IF NOT EXISTS submission_testcase_assertion_result (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_testcase_result_id UUID NOT NULL REFERENCES submission_testcase_result(id) ON DELETE CASCADE,
    testcase_assertion_id         UUID NOT NULL REFERENCES testcase_assertion(id) ON DELETE CASCADE,
    result                        testcase_result_status NOT NULL,
    actual_value                  JSONB,
    feedback                      TEXT,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT submission_testcase_assertion_result_key
        UNIQUE (submission_testcase_result_id, testcase_assertion_id)
);

CREATE INDEX IF NOT EXISTS idx_staresult_str_id
    ON submission_testcase_assertion_result(submission_testcase_result_id);

-- ========== PLAGIARISM ==========
CREATE TABLE IF NOT EXISTS submission_plagiarism_fingerprint (
    submission_id       UUID PRIMARY KEY REFERENCES lab_submission(id) ON DELETE CASCADE,
    lab_id              UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    git_commit_hashes   TEXT,
    metadata_canonical  TEXT,
    file_hashes         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plagiarism_fingerprint_lab_user
    ON submission_plagiarism_fingerprint (lab_id, user_id);

CREATE TABLE IF NOT EXISTS submission_plagiarism_match (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id               UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    submission_id        UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    other_submission_id  UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    git_match            BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_match       BOOLEAN NOT NULL DEFAULT FALSE,
    hash_similarity      NUMERIC(5, 2) NOT NULL DEFAULT 0,
    flagged              BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (submission_id, other_submission_id)
);

CREATE INDEX IF NOT EXISTS idx_plagiarism_match_lab_flagged
    ON submission_plagiarism_match (lab_id, flagged);
CREATE INDEX IF NOT EXISTS idx_plagiarism_match_submission
    ON submission_plagiarism_match (submission_id);
CREATE INDEX IF NOT EXISTS idx_plagiarism_match_other
    ON submission_plagiarism_match (other_submission_id);

-- ========== FEEDBACK INBOX ==========
CREATE TABLE IF NOT EXISTS feedback_item (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    author_irn   TEXT NOT NULL,
    author_name  TEXT NOT NULL,
    item_type    TEXT NOT NULL,
    message      TEXT NOT NULL,
    status       TEXT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at        TIMESTAMPTZ,
    lecturer_response  TEXT,
    response_at        TIMESTAMPTZ,
    archived_at        TIMESTAMPTZ,
    CONSTRAINT feedback_item_type_chk CHECK (item_type IN ('BUG', 'CONTRIBUTION')),
    CONSTRAINT feedback_item_status_chk CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX IF NOT EXISTS idx_feedback_item_status_created
    ON feedback_item (status, created_at DESC);

CREATE TABLE IF NOT EXISTS feedback_attachment (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_item_id     UUID NOT NULL REFERENCES feedback_item(id) ON DELETE CASCADE,
    original_filename    TEXT NOT NULL,
    stored_content_type  TEXT NOT NULL,
    byte_size            INTEGER NOT NULL,
    content              BYTEA NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_feedback_attachment_item
    ON feedback_attachment (feedback_item_id);

-- ========== MINIMAL SEED (roles + common master_data) ==========
INSERT INTO role (id, name)
SELECT gen_random_uuid(), v.name
FROM (VALUES ('STUDENT'), ('LECTURER'), ('TEACHER')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM role r WHERE r.name = v.name);

INSERT INTO master_data (id, name, category) VALUES
    (1,  'PUBLIC',           'SCOPE'),
    (2,  'PRIVATE',          'SCOPE'),
    (3,  'PROTECTED',        'SCOPE'),
    (4,  'DEFAULT',          'SCOPE'),
    (10, 'CLASS',            'DECLARING_TYPE'),
    (11, 'INTERFACE',        'DECLARING_TYPE'),
    (12, 'ABSTRACT_CLASS',   'DECLARING_TYPE'),
    (13, 'ENUM',             'DECLARING_TYPE'),
    (20, 'INHERITANCE',      'RELATION_TYPE'),
    (21, 'REALIZATION',      'RELATION_TYPE'),
    (22, 'ASSOCIATION',      'RELATION_TYPE'),
    (23, 'AGGREGATION',      'RELATION_TYPE'),
    (24, 'COMPOSITION',      'RELATION_TYPE'),
    (25, 'DEPENDENCY',       'RELATION_TYPE')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category;
