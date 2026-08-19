-- Plagiarism fingerprints and pairwise flags per lab submission.
-- Operator-run. Safe to re-run.

CREATE TABLE IF NOT EXISTS submission_plagiarism_fingerprint (
    submission_id UUID PRIMARY KEY REFERENCES lab_submission(id) ON DELETE CASCADE,
    lab_id UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    git_commit_hashes TEXT,
    metadata_canonical TEXT,
    file_hashes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plagiarism_fingerprint_lab_user
    ON submission_plagiarism_fingerprint (lab_id, user_id);

CREATE TABLE IF NOT EXISTS submission_plagiarism_match (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_id UUID NOT NULL REFERENCES lab(id) ON DELETE CASCADE,
    submission_id UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    other_submission_id UUID NOT NULL REFERENCES lab_submission(id) ON DELETE CASCADE,
    git_match BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_match BOOLEAN NOT NULL DEFAULT FALSE,
    hash_similarity NUMERIC(5, 2) NOT NULL DEFAULT 0,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (submission_id, other_submission_id)
);

CREATE INDEX IF NOT EXISTS idx_plagiarism_match_lab_flagged
    ON submission_plagiarism_match (lab_id, flagged);

CREATE INDEX IF NOT EXISTS idx_plagiarism_match_submission
    ON submission_plagiarism_match (submission_id);

CREATE INDEX IF NOT EXISTS idx_plagiarism_match_other
    ON submission_plagiarism_match (other_submission_id);
