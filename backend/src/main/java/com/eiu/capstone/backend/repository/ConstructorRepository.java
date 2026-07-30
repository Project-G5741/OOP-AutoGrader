package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Constructor;

public interface ConstructorRepository extends JpaRepository<Constructor, UUID> {

    List<Constructor> findByClassEntity(ClassEntity classEntity);

    @Query("SELECT c FROM Constructor c JOIN FETCH c.constructorDeclaration cd JOIN FETCH cd.scope WHERE c.classEntity IN :classEntities")
    List<Constructor> findByClassEntityInWithDeclaration(@Param("classEntities") List<ClassEntity> classEntities);
}