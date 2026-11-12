package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Stores MMD, Declaration, and Operational percentages on each graded attempt.
 * Desktop practice adds the same columns through Hibernate {@code ddl-auto: update}.
 */
@Component
@Profile("!desktop")
public class SubmissionPillarScoreSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public SubmissionPillarScoreSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensurePillarScoreColumns() {
        addNumeric("lab_submission", "mmd_score");
        addNumeric("lab_submission", "declaration_score");
        addNumeric("lab_submission", "operational_score");
        addNumeric("submission_challenge_result", "mmd_score");
        addNumeric("submission_challenge_result", "declaration_score");
        addNumeric("submission_challenge_result", "operational_score");
    }

    private void addNumeric(String table, String column) {
        jdbcTemplate.execute("""
                ALTER TABLE %s
                    ADD COLUMN IF NOT EXISTS %s NUMERIC(6, 2)
                """.formatted(table, column));
    }
}
