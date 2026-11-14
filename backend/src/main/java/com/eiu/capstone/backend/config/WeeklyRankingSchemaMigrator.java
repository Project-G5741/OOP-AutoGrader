package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class WeeklyRankingSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public WeeklyRankingSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureWeeklyRanking() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS weekly_ranking_week (
                    week_end  TIMESTAMPTZ PRIMARY KEY,
                    paid_at   TIMESTAMPTZ
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS weekly_ranking_entry (
                    week_end     TIMESTAMPTZ NOT NULL REFERENCES weekly_ranking_week(week_end) ON DELETE CASCADE,
                    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
                    rank         INTEGER NOT NULL,
                    total        NUMERIC(10, 2) NOT NULL,
                    gift_points  INTEGER NOT NULL,
                    gift_tickets INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (week_end, user_id)
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE weekly_ranking_entry
                    ADD COLUMN IF NOT EXISTS gift_tickets INTEGER NOT NULL DEFAULT 0
                """);
    }
}
