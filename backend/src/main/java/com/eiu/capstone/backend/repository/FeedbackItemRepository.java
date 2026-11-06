package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.FeedbackItem;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, UUID> {
    List<FeedbackItem> findByArchivedAtIsNullOrderByCreatedAtDesc();

    List<FeedbackItem> findByStatusAndArchivedAtIsNullOrderByCreatedAtDesc(String status);

    List<FeedbackItem> findByUser_IdAndLecturerResponseIsNotNullOrderByResponseAtDesc(UUID userId);

    List<FeedbackItem> findByIdInAndUser_IdAndLecturerResponseIsNotNull(Collection<UUID> ids, UUID userId);
}
