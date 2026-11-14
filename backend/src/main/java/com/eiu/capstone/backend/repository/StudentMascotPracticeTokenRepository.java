package com.eiu.capstone.backend.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.StudentMascotPracticeToken;

public interface StudentMascotPracticeTokenRepository extends JpaRepository<StudentMascotPracticeToken, UUID> {

    Optional<StudentMascotPracticeToken> findByJti(String jti);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StudentMascotPracticeToken t set t.usedAt = :now where t.jti = :jti and t.usedAt is null")
    int markUsedIfUnused(@Param("jti") String jti, @Param("now") Instant now);
}
