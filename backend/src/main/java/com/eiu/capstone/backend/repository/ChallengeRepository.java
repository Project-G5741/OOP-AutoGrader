package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    Optional<Challenge> findByLabAndChallengeNumber(Lab lab, Integer challengeNumber);

    List<Challenge> findByLabOrderByChallengeNumberAsc(Lab lab);
}