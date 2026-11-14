-- Operator-run: one live mascot-practice JWT per student for desktop snapshot pull.
-- Startup migrator MascotPracticeTokenSchemaMigrator applies the same statements on PostgreSQL.

CREATE TABLE IF NOT EXISTS student_mascot_practice_token (
    user_id        UUID PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    jti            VARCHAR(64) NOT NULL,
    compact_token  VARCHAR(2048) NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS student_mascot_practice_token_jti_idx
    ON student_mascot_practice_token (jti);
