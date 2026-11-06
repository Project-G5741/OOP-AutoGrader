-- Mascot shop: equipped mascot on the streak row, plus one purchase row per bought mascot.
-- Operator-run. Safe to re-run. Startup migrator applies the same statements on PostgreSQL.

ALTER TABLE student_daily_streak
    ADD COLUMN IF NOT EXISTS equipped_mascot VARCHAR(32) NOT NULL DEFAULT 'fox';

CREATE TABLE IF NOT EXISTS student_mascot_purchase (
    user_id       UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    mascot_id     VARCHAR(32) NOT NULL,
    price_paid    INTEGER NOT NULL,
                    purchased_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, mascot_id)
);
