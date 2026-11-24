package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class HolidayScheduleConfigSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public HolidayScheduleConfigSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS holiday_schedule_config (
                    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
                    config_json     TEXT NOT NULL DEFAULT '{}',
                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO holiday_schedule_config (id, config_json) VALUES (1, '{}')
                ON CONFLICT (id) DO NOTHING
                """);
    }
}
