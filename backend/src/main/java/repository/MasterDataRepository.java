package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.MasterData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MasterDataRepository extends JpaRepository<MasterData, Integer> {

    List<MasterData> findByCategoryOrderByNameAsc(String category);
}
