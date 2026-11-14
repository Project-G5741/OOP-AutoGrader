package com.eiu.capstone.backend.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Desktop H2 counterpart to {@link DeclarationAliasListsSchemaMigrator}.
 * Hibernate {@code ddl-auto: update} creates the alias child entities; this migrator
 * backfills order_index 0 from primary columns when a parent still has no child rows
 * (existing practice DBs upgraded in place). Load paths already fall back to the
 * primary column when children are missing; backfill keeps H2 aligned with cloud.
 */
@Component
@Profile("desktop")
public class DesktopDeclarationAliasListsSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    public DesktopDeclarationAliasListsSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureAliasLists() {
        createTablesIfMissing();
        backfill();
    }

    private void createTablesIfMissing() {
        // H2 PostgreSQL mode: RANDOM_UUID(); quote "value" (reserved).
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS field_declaration_name_alias (
                    id                   UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    field_declaration_id UUID NOT NULL,
                    "value"              VARCHAR(255) NOT NULL,
                    order_index          INTEGER NOT NULL,
                    CONSTRAINT fk_fd_name_alias_field
                        FOREIGN KEY (field_declaration_id) REFERENCES field_declaration(id) ON DELETE CASCADE,
                    CONSTRAINT uq_fd_name_alias UNIQUE (field_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS field_declaration_type_alias (
                    id                   UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    field_declaration_id UUID NOT NULL,
                    "value"              VARCHAR(255),
                    order_index          INTEGER NOT NULL,
                    CONSTRAINT fk_fd_type_alias_field
                        FOREIGN KEY (field_declaration_id) REFERENCES field_declaration(id) ON DELETE CASCADE,
                    CONSTRAINT uq_fd_type_alias UNIQUE (field_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS method_declaration_name_alias (
                    id                    UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    method_declaration_id UUID NOT NULL,
                    "value"               VARCHAR(255) NOT NULL,
                    order_index           INTEGER NOT NULL,
                    CONSTRAINT fk_md_name_alias_method
                        FOREIGN KEY (method_declaration_id) REFERENCES method_declaration(id) ON DELETE CASCADE,
                    CONSTRAINT uq_md_name_alias UNIQUE (method_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS method_declaration_return_type_alias (
                    id                    UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    method_declaration_id UUID NOT NULL,
                    "value"               VARCHAR(255) NOT NULL,
                    order_index           INTEGER NOT NULL,
                    CONSTRAINT fk_md_ret_alias_method
                        FOREIGN KEY (method_declaration_id) REFERENCES method_declaration(id) ON DELETE CASCADE,
                    CONSTRAINT uq_md_ret_alias UNIQUE (method_declaration_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parameter_name_alias (
                    id           UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    parameter_id UUID NOT NULL,
                    "value"      VARCHAR(255) NOT NULL,
                    order_index  INTEGER NOT NULL,
                    CONSTRAINT fk_param_name_alias_param
                        FOREIGN KEY (parameter_id) REFERENCES parameter(id) ON DELETE CASCADE,
                    CONSTRAINT uq_param_name_alias UNIQUE (parameter_id, order_index)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS parameter_type_alias (
                    id           UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
                    parameter_id UUID NOT NULL,
                    "value"      VARCHAR(255) NOT NULL,
                    order_index  INTEGER NOT NULL,
                    CONSTRAINT fk_param_type_alias_param
                        FOREIGN KEY (parameter_id) REFERENCES parameter(id) ON DELETE CASCADE,
                    CONSTRAINT uq_param_type_alias UNIQUE (parameter_id, order_index)
                )
                """);
    }

    private void backfill() {
        jdbcTemplate.update("""
                INSERT INTO field_declaration_name_alias (id, field_declaration_id, "value", order_index)
                SELECT RANDOM_UUID(), fd.id, fd.name, 0
                FROM field_declaration fd
                WHERE NOT EXISTS (
                    SELECT 1 FROM field_declaration_name_alias a WHERE a.field_declaration_id = fd.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO field_declaration_type_alias (id, field_declaration_id, "value", order_index)
                SELECT RANDOM_UUID(), fd.id, fd.data_type, 0
                FROM field_declaration fd
                WHERE NOT EXISTS (
                    SELECT 1 FROM field_declaration_type_alias a WHERE a.field_declaration_id = fd.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO method_declaration_name_alias (id, method_declaration_id, "value", order_index)
                SELECT RANDOM_UUID(), md.id, md.name, 0
                FROM method_declaration md
                WHERE NOT EXISTS (
                    SELECT 1 FROM method_declaration_name_alias a WHERE a.method_declaration_id = md.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO method_declaration_return_type_alias (id, method_declaration_id, "value", order_index)
                SELECT RANDOM_UUID(), md.id, md.return_type, 0
                FROM method_declaration md
                WHERE NOT EXISTS (
                    SELECT 1 FROM method_declaration_return_type_alias a WHERE a.method_declaration_id = md.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO parameter_name_alias (id, parameter_id, "value", order_index)
                SELECT RANDOM_UUID(), p.id, p.name, 0
                FROM parameter p
                WHERE NOT EXISTS (
                    SELECT 1 FROM parameter_name_alias a WHERE a.parameter_id = p.id
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO parameter_type_alias (id, parameter_id, "value", order_index)
                SELECT RANDOM_UUID(), p.id, p.data_type, 0
                FROM parameter p
                WHERE NOT EXISTS (
                    SELECT 1 FROM parameter_type_alias a WHERE a.parameter_id = p.id
                )
                """);
    }
}
