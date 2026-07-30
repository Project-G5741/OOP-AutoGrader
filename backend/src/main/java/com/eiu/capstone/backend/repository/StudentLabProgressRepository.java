package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.StudentLabProgress;
import com.eiu.capstone.backend.model.UserAccount;

public interface StudentLabProgressRepository extends JpaRepository<StudentLabProgress, UUID> {

    Optional<StudentLabProgress> findByUserAndLab(UserAccount user, Lab lab);
}