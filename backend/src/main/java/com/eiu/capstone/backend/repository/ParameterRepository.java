package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParameterRepository extends JpaRepository<Parameter, UUID> {
}
