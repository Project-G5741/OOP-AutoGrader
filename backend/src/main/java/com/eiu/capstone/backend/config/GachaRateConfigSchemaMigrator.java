package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class GachaRateConfigSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public GachaRateConfigSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS gacha_rate_config (
                    id                      SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
                    cost_single_points      INTEGER NOT NULL DEFAULT 100,
                    cost_multi_points       INTEGER NOT NULL DEFAULT 900,
                    hard_pity_limit         INTEGER NOT NULL DEFAULT 100,
                    soft_pity_start         INTEGER NOT NULL DEFAULT 90,
                    direct_legendary_rate   DOUBLE PRECISION NOT NULL DEFAULT 0.00016666666666666666,
                    crystal_base_rate       DOUBLE PRECISION NOT NULL DEFAULT 0.0015,
                    random_mascot_rate      DOUBLE PRECISION NOT NULL DEFAULT 0.003,
                    cashback_rate_300       DOUBLE PRECISION NOT NULL DEFAULT 0.025,
                    cashback_rate_150       DOUBLE PRECISION NOT NULL DEFAULT 0.06,
                    cashback_rate_60        DOUBLE PRECISION NOT NULL DEFAULT 0.18,
                    cashback_rate_20        DOUBLE PRECISION NOT NULL DEFAULT 0.42,
                    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO gacha_rate_config (id) VALUES (1)
                ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS gacha_pity_count INTEGER NOT NULL DEFAULT 0
                """);
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS crystal_sourced_points INTEGER NOT NULL DEFAULT 0
                """);
        jdbcTemplate.update("""
                UPDATE gacha_rate_config
                   SET soft_pity_start = 90
                 WHERE id = 1
                   AND soft_pity_start < 90
                """);
    }
}