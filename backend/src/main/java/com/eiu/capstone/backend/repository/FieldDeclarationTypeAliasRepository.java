package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.FieldDeclarationTypeAlias;

public interface FieldDeclarationTypeAliasRepository extends JpaRepository<FieldDeclarationTypeAlias, UUID> {

    List<FieldDeclarationTypeAlias> findByFieldDeclaration_IdOrderByOrderIndexAsc(UUID fieldDeclarationId);

    List<FieldDeclarationTypeAlias> findByFieldDeclaration_IdInOrderByOrderIndexAsc(Collection<UUID> fieldDeclarationIds);

    @Modifying
    @Transactional
    void deleteByFieldDeclaration_IdIn(Collection<UUID> fieldDeclarationIds);
}
