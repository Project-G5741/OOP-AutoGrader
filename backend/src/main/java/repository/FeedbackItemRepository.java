package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.FeedbackItem;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, UUID> {
    List<FeedbackItem> findAllByOrderByCreatedAtDesc();

    List<FeedbackItem> findByStatusOrderByCreatedAtDesc(String status);

    List<FeedbackItem> findByUser_IdAndLecturerResponseIsNotNullOrderByResponseAtDesc(UUID userId);
}
