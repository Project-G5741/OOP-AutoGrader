-- Operator-run: add key_invalid flag to ai_student_credential.
-- Set true by the backend when Gemini rejects the stored API key (HTTP 400/401/403);
-- cleared automatically when the student imports a valid replacement key.
-- Startup migrator AiStudentCredentialSchemaMigrator applies the same statement on PostgreSQL.

ALTER TABLE ai_student_credential
    ADD COLUMN IF NOT EXISTS key_invalid BOOLEAN NOT NULL DEFAULT false;
