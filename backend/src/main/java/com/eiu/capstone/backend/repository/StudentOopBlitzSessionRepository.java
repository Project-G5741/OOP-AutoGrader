package com.eiu.capstone.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.StudentOopBlitzSession;

import jakarta.persistence.LockModeType;

public interface StudentOopBlitzSessionRepository extends JpaRepository<StudentOopBlitzSession, UUID> {

    Optional<StudentOopBlitzSession> findFirstByUserIdAndStatus(UUID userId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudentOopBlitzSession s where s.id = :id and s.userId = :userId")
    Optional<StudentOopBlitzSession> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    List<StudentOopBlitzSession> findByUserIdAndPlayDate(UUID userId, LocalDate playDate);

    List<StudentOopBlitzSession> findTop12ByUserIdOrderByStartedAtDesc(UUID userId);
}
