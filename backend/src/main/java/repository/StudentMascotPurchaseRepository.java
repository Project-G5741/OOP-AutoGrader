package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;

public interface StudentMascotPurchaseRepository extends JpaRepository<StudentMascotPurchase, StudentMascotPurchaseKey> {

    List<StudentMascotPurchase> findByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdMascotId(UUID userId, String mascotId);
}
