package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.OopBlitzQuestion;

public interface OopBlitzQuestionRepository extends JpaRepository<OopBlitzQuestion, UUID> {

    List<OopBlitzQuestion> findByActiveTrueOrderByUpdatedAtDesc();

    List<OopBlitzQuestion> findAllByOrderByUpdatedAtDesc();
}
