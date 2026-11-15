package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationReadSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public StudentNotificationReadSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureNotificationReadTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS student_notification_read (
                    user_id    UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
                    notif_key  VARCHAR(191) NOT NULL,
                    read_at    TIMESTAMPTZ NOT NULL,
                    PRIMARY KEY (user_id, notif_key)
                )
                """);
    }
}
