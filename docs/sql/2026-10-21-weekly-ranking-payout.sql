-- Weekly ranking freeze + one-time shop-point and gacha-ticket gifts (Sunday 23:59:59 Vietnam).
-- Startup migrator WeeklyRankingSchemaMigrator applies the same statements on PostgreSQL.

CREATE TABLE IF NOT EXISTS weekly_ranking_week (
    week_end  TIMESTAMPTZ PRIMARY KEY,
    paid_at   TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS weekly_ranking_entry (
    week_end     TIMESTAMPTZ NOT NULL REFERENCES weekly_ranking_week(week_end) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    rank         INTEGER NOT NULL,
    total        NUMERIC(10, 2) NOT NULL,
    gift_points  INTEGER NOT NULL,
    gift_tickets INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (week_end, user_id)
);

ALTER TABLE weekly_ranking_entry
    ADD COLUMN IF NOT EXISTS gift_tickets INTEGER NOT NULL DEFAULT 0;
