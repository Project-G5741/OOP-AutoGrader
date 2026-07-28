package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Field;

public interface FieldRepository extends JpaRepository<Field, UUID> {

    List<Field> findByClassEntity(ClassEntity classEntity);

    @Query("SELECT f FROM Field f JOIN FETCH f.fieldDeclaration fd JOIN FETCH fd.scope WHERE f.classEntity IN :classEntities")
    List<Field> findByClassEntityInWithDeclaration(@Param("classEntities") List<ClassEntity> classEntities);
}