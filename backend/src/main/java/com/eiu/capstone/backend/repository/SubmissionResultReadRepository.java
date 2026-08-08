package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubmissionResultReadRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SubmissionResultReadRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, List<UUID>> findCorrectIdsByType(UUID submissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("submissionId", submissionId);
        return jdbc.query("""
                SELECT 'field' AS result_type, field_id AS entity_id
                FROM submission_field_result
                WHERE submission_id = :submissionId AND is_correct = true
                UNION ALL
                SELECT 'method', method_id
                FROM submission_method_result
                WHERE submission_id = :submissionId AND is_correct = true
                UNION ALL
                SELECT 'constructor', constructor_id
                FROM submission_constructor_result
                WHERE submission_id = :submissionId AND is_correct = true
                UNION ALL
                SELECT 'relation', class_relation_id
                FROM submission_relation_result
                WHERE submission_id = :submissionId AND is_correct = true
                """, params, (rs, rowNum) -> Map.entry(
                rs.getString("result_type"),
                rs.getObject("entity_id", UUID.class)))
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public boolean hasAnyResults(UUID submissionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("submissionId", submissionId);
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT 1 FROM submission_field_result WHERE submission_id = :submissionId
                    UNION ALL
                    SELECT 1 FROM submission_method_result WHERE submission_id = :submissionId
                    UNION ALL
                    SELECT 1 FROM submission_constructor_result WHERE submission_id = :submissionId
                    UNION ALL
                    SELECT 1 FROM submission_relation_result WHERE submission_id = :submissionId
                ) t
                """, params, Integer.class);
        return count != null && count > 0;
    }
}
