package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Offline practice does not run streak/missions/shop or the web notification bell.
 * Hibernate {@code ddl-auto: update} may still create empty {@code student_daily_streak} /
 * {@code student_mascot_purchase} / {@code student_notification_read} /
 * {@code student_notification_dismiss} from shared entities;
 * drop them after schema update so leftover installs lose that state.
 * {@code @Lazy(false)} is required because desktop enables {@code spring.main.lazy-initialization}.
 */
@Component
@Lazy(false)
@Profile({"desktop", "desktop-lecturer"})
public class DesktopStreakSchemaCleanupMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DesktopStreakSchemaCleanupMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void dropStreakAndShopTables() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS oop_blitz_question");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_oop_blitz_session");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_notification_dismiss");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_notification_read");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_mascot_purchase");
        jdbcTemplate.execute("DROP TABLE IF EXISTS student_daily_streak");
    }
}
