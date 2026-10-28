package com.eiu.capstone.backend.config;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FieldDeclarationModifiersSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public FieldDeclarationModifiersSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void addModifierColumns() {
        addBooleanColumn("is_static");
        addBooleanColumn("is_final");
    }

    private void addBooleanColumn(String column) {
        if (columnExists(column)) {
            return;
        }
        jdbcTemplate.execute(
                "ALTER TABLE field_declaration ADD COLUMN " + column + " BOOLEAN NOT NULL DEFAULT false");
    }

    private boolean columnExists(String column) {
        List<String> names = jdbcTemplate.query("""
                SELECT column_name FROM information_schema.columns
                WHERE lower(table_name) = 'field_declaration'
                  AND lower(column_name) = ?
                """, (rs, rowNum) -> rs.getString(1), column);
        return !names.isEmpty();
    }
}
