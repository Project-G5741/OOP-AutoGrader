package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, UUID> {

    @Query("SELECT c FROM ClassEntity c JOIN FETCH c.scope JOIN FETCH c.declaringType WHERE c.challenge IN :challenges")
    List<ClassEntity> findByChallengeInWithAttributes(@Param("challenges") List<Challenge> challenges);
}