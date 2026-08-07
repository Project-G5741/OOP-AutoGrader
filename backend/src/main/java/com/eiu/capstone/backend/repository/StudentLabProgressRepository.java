package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.model.UserAccount;

public interface StudentLabProgressRepository extends JpaRepository<StudentLabProgress, UUID> {

    Optional<StudentLabProgress> findByUserAndLab(UserAccount user, Lab lab);

    Optional<StudentLabProgress> findByUser_IdAndLab_Id(UUID userId, UUID labId);

    @Query("SELECT p FROM StudentLabProgress p JOIN FETCH p.lab WHERE p.user.id = :userId ORDER BY p.lastSubmittedAt DESC")
    List<StudentLabProgress> findByUser_IdWithLabOrderByLastSubmittedAtDesc(@Param("userId") UUID userId);
}