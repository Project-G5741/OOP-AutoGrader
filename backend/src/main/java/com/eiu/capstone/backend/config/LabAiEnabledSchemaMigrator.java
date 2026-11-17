package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Adds {@code lab.ai_enabled} on older PostgreSQL databases.
 * Desktop practice creates the column through Hibernate {@code ddl-auto: update}.
 */
@Component
@Profile("!desktop & !desktop-lecturer")
public class LabAiEnabledSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public LabAiEnabledSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureLabAiEnabled() {
        jdbcTemplate.execute("""
                ALTER TABLE lab
                    ADD COLUMN IF NOT EXISTS ai_enabled BOOLEAN NOT NULL DEFAULT false
                """);
    }
}
