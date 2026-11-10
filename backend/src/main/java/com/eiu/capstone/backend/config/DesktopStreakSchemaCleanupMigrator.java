package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Offline practice does not run streak/missions/shop. Hibernate {@code ddl-auto: update}
 * may still create empty {@code student_daily_streak} / {@code student_mascot_purchase}
 * from shared entities; drop them after schema update so leftover installs lose that state.
 */
@Component
@Profile("desktop")
public class DesktopStreakSchemaCleanupMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DesktopStreakSchemaCleanupMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void dropStreakAndShopTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_mascot_purchase");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_daily_streak");
    }
}
