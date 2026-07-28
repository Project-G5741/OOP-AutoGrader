package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FieldRepository extends JpaRepository<Field, UUID> {
}
