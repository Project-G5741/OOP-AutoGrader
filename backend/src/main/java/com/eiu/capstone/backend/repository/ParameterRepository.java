package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;

public interface ParameterRepository extends JpaRepository<Parameter, UUID> {

    List<Parameter> findByMethodOrderByOrderIndexAsc(Method method);
    List<Parameter> findByConstructorEntityOrderByOrderIndexAsc(Constructor constructorEntity);

    List<Parameter> findByMethodIn(List<Method> methods);
    List<Parameter> findByConstructorEntityIn(List<Constructor> constructorEntities);
}