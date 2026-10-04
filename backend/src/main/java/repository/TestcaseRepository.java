package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Testcase;

public interface TestcaseRepository extends JpaRepository<Testcase, UUID> {

    List<Testcase> findByChallenge_IdInOrderByOrderIndexAsc(Collection<UUID> challengeIds);

    List<Testcase> findByChallenge_IdOrderByOrderIndexAsc(UUID challengeId);
}
