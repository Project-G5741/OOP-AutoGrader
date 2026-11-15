package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.StudentNotificationRead;
import com.eiu.capstone.backend.model.StudentNotificationReadKey;

public interface StudentNotificationReadRepository
        extends JpaRepository<StudentNotificationRead, StudentNotificationReadKey> {

    List<StudentNotificationRead> findByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdNotifKey(UUID userId, String notifKey);
}
