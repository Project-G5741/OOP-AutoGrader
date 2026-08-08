-- Analytics performance indexes for OOP AutoGrader (Neon PostgreSQL).
-- Run as database owner. CREATE INDEX CONCURRENTLY cannot run inside a transaction block.
-- Optional scale path (not applied at current row counts):
--   CREATE EXTENSION IF NOT EXISTS pg_trgm;
--   CREATE INDEX CONCURRENTLY idx_user_full_name_trgm ON user_account USING gin (lower(full_name) gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_term_enrollment_term_id
  ON term_enrollment (term_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lab_submission_submitted_at_desc
  ON lab_submission (submitted_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_slp_active_submitters
  ON student_lab_progress (user_id)
  WHERE last_submitted_at IS NOT NULL;
