package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.DTO.LecturerMascotOwnershipRow;

@Service
@Profile("!desktop & !desktop-lecturer")
public class LecturerMascotOwnershipService {

    private final JdbcTemplate jdbcTemplate;

    public LecturerMascotOwnershipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<LecturerMascotOwnershipRow> listCurrentTermStudents() {
        List<UUID> studentIds = jdbcTemplate.query("""
                SELECT DISTINCT ua.id
                FROM term_enrollment te
                JOIN user_account ua ON ua.id = te.user_id
                JOIN user_role ur ON ur.user_id = ua.id
                JOIN role r ON r.id = ur.role_id
                JOIN term t ON t.id = te.term_id AND t.is_current = TRUE
                WHERE LOWER(r.name) = 'student' AND ua.is_active = TRUE
                ORDER BY ua.student_code
                """, (rs, rowNum) -> rs.getObject(1, UUID.class));

        List<LecturerMascotOwnershipRow> rows = new ArrayList<>();
        for (UUID userId : studentIds) {
            rows.add(loadRow(userId));
        }
        return rows;
    }

    private LecturerMascotOwnershipRow loadRow(UUID userId) {
        var profile = jdbcTemplate.queryForObject("""
                SELECT student_code, full_name
                FROM user_account WHERE id = ?
                """, (rs, rowNum) -> new String[] { rs.getString(1), rs.getString(2) }, userId);

        String equipped = jdbcTemplate.query("""
                SELECT equipped_mascot FROM student_daily_streak WHERE user_id = ?
                """, rs -> rs.next() ? rs.getString(1) : "fox", userId);

        List<LecturerMascotOwnershipRow.OwnedMascot> owned = jdbcTemplate.query("""
                SELECT mascot_id, price_paid, purchased_at
                FROM student_mascot_purchase
                WHERE user_id = ?
                ORDER BY purchased_at
                """, (rs, rowNum) -> new LecturerMascotOwnershipRow.OwnedMascot(
                rs.getString(1),
                MascotCatalog.displayName(rs.getString(1)),
                rs.getInt(2),
                rs.getObject(3, java.time.OffsetDateTime.class)), userId);

        owned.sort(Comparator.comparing(LecturerMascotOwnershipRow.OwnedMascot::mascotId));
        return new LecturerMascotOwnershipRow(profile[0], profile[1], equipped, owned);
    }
}