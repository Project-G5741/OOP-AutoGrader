package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, UUID> {
}
