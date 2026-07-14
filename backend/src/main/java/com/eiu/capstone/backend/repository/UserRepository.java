package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}