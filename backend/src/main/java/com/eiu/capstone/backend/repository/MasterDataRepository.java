package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.MasterData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterDataRepository extends JpaRepository<MasterData, Integer> {
}
