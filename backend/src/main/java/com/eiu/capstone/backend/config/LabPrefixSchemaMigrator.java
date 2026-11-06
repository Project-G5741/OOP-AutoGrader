package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Adds {@code lab.prefix} on older PostgreSQL databases.
 * Desktop practice creates the column through Hibernate {@code ddl-auto: update}.
 */
@Component
@Profile("!desktop")
public class LabPrefixSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public LabPrefixSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureLabPrefix() {
        jdbcTemplate.execute("""
                ALTER TABLE lab
                    ADD COLUMN IF NOT EXISTS prefix VARCHAR(16) NOT NULL DEFAULT 'Official'
                """);
    }
}
