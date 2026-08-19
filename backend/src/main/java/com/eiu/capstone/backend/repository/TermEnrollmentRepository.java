package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.TermEnrollment;

public interface TermEnrollmentRepository extends JpaRepository<TermEnrollment, UUID> {
    long countByTerm_Id(UUID termId);

    @Query(value = """
            SELECT DISTINCT u.id
            FROM term_enrollment te
            JOIN user_account u ON u.id = te.user_id
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE te.term_id = :termId
              AND u.is_active = true
              AND LOWER(r.name) = 'student'
            """, nativeQuery = true)
    List<UUID> findActiveStudentIdsByTermId(@Param("termId") UUID termId);
}
