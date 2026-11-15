package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationDismissSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public StudentNotificationDismissSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureNotificationDismissTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS student_notification_dismiss (
                    user_id      UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
                    notif_key    VARCHAR(191) NOT NULL,
                    dismissed_at TIMESTAMPTZ NOT NULL,
                    PRIMARY KEY (user_id, notif_key)
                )
                """);
    }
}
