package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.MethodDeclarationReturnTypeAlias;

public interface MethodDeclarationReturnTypeAliasRepository
        extends JpaRepository<MethodDeclarationReturnTypeAlias, UUID> {

    List<MethodDeclarationReturnTypeAlias> findByMethodDeclaration_IdOrderByOrderIndexAsc(UUID methodDeclarationId);

    List<MethodDeclarationReturnTypeAlias> findByMethodDeclaration_IdInOrderByOrderIndexAsc(
            Collection<UUID> methodDeclarationIds);

    @Modifying
    @Transactional
    void deleteByMethodDeclaration_IdIn(Collection<UUID> methodDeclarationIds);
}
