package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.ClassRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassRelationRepository extends JpaRepository<ClassRelation, UUID> {
}
