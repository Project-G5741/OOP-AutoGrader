package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.ParameterNameAlias;

public interface ParameterNameAliasRepository extends JpaRepository<ParameterNameAlias, UUID> {

    List<ParameterNameAlias> findByParameter_IdOrderByOrderIndexAsc(UUID parameterId);

    List<ParameterNameAlias> findByParameter_IdInOrderByOrderIndexAsc(Collection<UUID> parameterIds);

    @Modifying
    @Transactional
    void deleteByParameter_IdIn(Collection<UUID> parameterIds);
}
