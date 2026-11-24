package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Adds {@code egg_smash_pity} to {@code student_daily_streak} on PostgreSQL.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class EggSmashSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public EggSmashSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                ALTER TABLE student_daily_streak
                    ADD COLUMN IF NOT EXISTS egg_smash_pity INTEGER NOT NULL DEFAULT 0
                """);
    }
}
