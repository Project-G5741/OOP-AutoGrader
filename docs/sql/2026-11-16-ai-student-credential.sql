-- Per-student Gemini BYOK credential (encrypted) + token budget tracking.
-- Startup migrator AiStudentCredentialSchemaMigrator applies the same CREATE on PostgreSQL.

CREATE TABLE IF NOT EXISTS ai_student_credential (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    key_hint VARCHAR(16) NOT NULL,
    tokens_used BIGINT NOT NULL DEFAULT 0,
    token_budget BIGINT NOT NULL DEFAULT 1000000,
    notified_50 BOOLEAN NOT NULL DEFAULT false,
    notified_80 BOOLEAN NOT NULL DEFAULT false,
    notified_90 BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ai_student_credential_student UNIQUE (student_id)
);
