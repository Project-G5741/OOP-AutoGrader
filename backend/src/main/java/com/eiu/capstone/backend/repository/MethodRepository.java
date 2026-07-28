package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Method;

public interface MethodRepository extends JpaRepository<Method, UUID> {

    List<Method> findByClassEntity(ClassEntity classEntity);

    @Query("SELECT m FROM Method m JOIN FETCH m.methodDeclaration md JOIN FETCH md.scope WHERE m.classEntity IN :classEntities")
    List<Method> findByClassEntityInWithDeclaration(@Param("classEntities") List<ClassEntity> classEntities);
}