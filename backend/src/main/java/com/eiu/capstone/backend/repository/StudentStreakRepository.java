package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.StudentStreak;

import jakarta.persistence.LockModeType;

public interface StudentStreakRepository extends JpaRepository<StudentStreak, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StudentStreak s where s.userId = :userId")
    Optional<StudentStreak> lockByUserId(@Param("userId") UUID userId);
}
