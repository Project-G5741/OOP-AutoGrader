package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.FeedbackAttachment;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, UUID> {
    Optional<FeedbackAttachment> findByIdAndItem_Id(UUID id, UUID itemId);

    long countByItem_Id(UUID itemId);

    @Query("""
            SELECT a.id AS id, a.originalFilename AS originalFilename,
                   a.storedContentType AS storedContentType, a.byteSize AS byteSize
            FROM FeedbackAttachment a
            WHERE a.item.id = :itemId
            """)
    List<FeedbackAttachmentMeta> findMetaByItemId(@Param("itemId") UUID itemId);

    interface FeedbackAttachmentMeta {
        UUID getId();

        String getOriginalFilename();

        String getStoredContentType();

        int getByteSize();
    }
}
