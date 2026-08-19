package com.eiu.capstone.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.LabDeadlineEmailSent;

public interface LabDeadlineEmailSentRepository extends JpaRepository<LabDeadlineEmailSent, UUID> {

    boolean existsByLab_IdAndUser_IdAndThresholdHours(UUID labId, UUID userId, short thresholdHours);
}
