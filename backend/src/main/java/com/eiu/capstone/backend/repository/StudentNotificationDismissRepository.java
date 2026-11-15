package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.StudentNotificationDismiss;
import com.eiu.capstone.backend.model.StudentNotificationDismissKey;

public interface StudentNotificationDismissRepository
        extends JpaRepository<StudentNotificationDismiss, StudentNotificationDismissKey> {

    List<StudentNotificationDismiss> findByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdNotifKey(UUID userId, String notifKey);
}
