-- Student daily streak and reward points.
-- Operator-run. Safe to re-run. Startup migrator applies the same statement on PostgreSQL.

CREATE TABLE IF NOT EXISTS student_daily_streak (
    user_id             UUID PRIMARY KEY REFERENCES user_account(id) ON DELETE CASCADE,
    streak_count        INTEGER NOT NULL DEFAULT 0,
    points              INTEGER NOT NULL DEFAULT 0,
    last_completed_on   DATE,
    mission_date        DATE,
    logged_in           BOOLEAN NOT NULL DEFAULT FALSE,
    submit_count        INTEGER NOT NULL DEFAULT 0,
    pet_count           INTEGER NOT NULL DEFAULT 0,
    awarded_today       BOOLEAN NOT NULL DEFAULT FALSE,
    streak_broken       BOOLEAN NOT NULL DEFAULT FALSE,
    saved_streak        INTEGER NOT NULL DEFAULT 0,
    restore_year_month  VARCHAR(7)
);
