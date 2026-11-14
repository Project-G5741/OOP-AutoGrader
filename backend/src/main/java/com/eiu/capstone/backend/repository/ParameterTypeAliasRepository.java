package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.ParameterTypeAlias;

public interface ParameterTypeAliasRepository extends JpaRepository<ParameterTypeAlias, UUID> {

    List<ParameterTypeAlias> findByParameter_IdOrderByOrderIndexAsc(UUID parameterId);

    List<ParameterTypeAlias> findByParameter_IdInOrderByOrderIndexAsc(Collection<UUID> parameterIds);

    @Modifying
    @Transactional
    void deleteByParameter_IdIn(Collection<UUID> parameterIds);
}
