package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Public read ({@code GET /api/auth/holiday-config}) and Admin-only write
 * ({@code PUT /api/lecturer/holiday-config}) for early holiday schedule settings.
 */
@RestController
@Profile("!desktop & !desktop-lecturer")
public class HolidayScheduleConfigController {

    private static final int MAX_CONFIG_JSON_LENGTH = 16384;

    private final JdbcTemplate jdbcTemplate;
    private final JwtAuthHelper jwtAuthHelper;
    private final ObjectMapper objectMapper;

    public HolidayScheduleConfigController(
            JdbcTemplate jdbcTemplate,
            JwtAuthHelper jwtAuthHelper,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtAuthHelper = jwtAuthHelper;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/api/auth/holiday-config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getHolidayConfig() {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT config_json FROM holiday_schedule_config WHERE id = 1",
                    (rs, rowNum) -> rs.getString("config_json"));
            String json = (rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank())
                    ? "{}"
                    : rows.get(0);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception ex) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}");
        }
    }

    @PutMapping(
            value = "/api/lecturer/holiday-config",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateHolidayConfig(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody String rawJson) {
        jwtAuthHelper.requireActiveUser(principal);
        if (!principal.hasRole(JwtRoleNames.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
        if (rawJson == null || rawJson.isBlank() || rawJson.length() > MAX_CONFIG_JSON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid holiday config payload");
        }
        final String normalizedJson;
        try {
            JsonNode tree = objectMapper.readTree(rawJson);
            if (tree == null || !tree.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Holiday config must be a JSON object");
            }
            normalizedJson = objectMapper.writeValueAsString(tree);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON payload");
        }

        jdbcTemplate.update("""
                INSERT INTO holiday_schedule_config (id, config_json, updated_at)
                VALUES (1, ?, NOW())
                ON CONFLICT (id) DO UPDATE
                   SET config_json = EXCLUDED.config_json,
                       updated_at  = NOW()
                """, normalizedJson);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(normalizedJson);
    }
}
