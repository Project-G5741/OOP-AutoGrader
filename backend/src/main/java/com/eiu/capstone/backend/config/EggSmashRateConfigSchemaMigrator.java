package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Ensures {@code egg_smash_rate_config} singleton row {@code id = 1} exists on PostgreSQL.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class EggSmashRateConfigSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public EggSmashRateConfigSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS egg_smash_rate_config (
                    id                      SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
                    base_crystal_hit_rate   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                    turn6_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                    turn7_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                    turn8_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 0.3333333333333333,
                    turn9_crystal_hit_rate  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
                    egg_rate_300            DOUBLE PRECISION NOT NULL DEFAULT 0.05,
                    egg_rate_200            DOUBLE PRECISION NOT NULL DEFAULT 0.15,
                    egg_rate_100            DOUBLE PRECISION NOT NULL DEFAULT 0.30,
                    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("""
                ALTER TABLE egg_smash_rate_config
                    ALTER COLUMN turn6_crystal_hit_rate SET DEFAULT 0.0,
                    ALTER COLUMN turn7_crystal_hit_rate SET DEFAULT 0.0
                """);
        jdbcTemplate.update("""
                INSERT INTO egg_smash_rate_config (id) VALUES (1)
                ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.update("""
                UPDATE egg_smash_rate_config
                SET turn6_crystal_hit_rate = 0.0,
                    turn7_crystal_hit_rate = 0.0,
                    updated_at = NOW()
                WHERE id = 1
                  AND ABS(turn6_crystal_hit_rate - 0.0001) < 1e-9
                  AND ABS(turn7_crystal_hit_rate - 0.05) < 1e-9
                """);
    }
}
