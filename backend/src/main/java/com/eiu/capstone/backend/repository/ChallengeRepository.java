package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    List<Challenge> findByLab_Id(UUID labId);
}
