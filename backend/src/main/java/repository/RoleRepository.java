package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByNameIn(Collection<String> names);
    Optional<Role> findByName(String name);
}
