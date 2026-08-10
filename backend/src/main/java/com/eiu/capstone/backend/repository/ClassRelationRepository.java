package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ClassRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClassRelationRepository extends JpaRepository<ClassRelation, UUID> {

    @Query("SELECT r FROM ClassRelation r "
            + "JOIN FETCH r.classEntity "
            + "JOIN FETCH r.targetClassEntity "
            + "JOIN FETCH r.relationType "
            + "WHERE r.classEntity IN :classEntities")
    List<ClassRelation> findByClassEntityInWithEndpoints(@Param("classEntities") List<ClassEntity> classEntities);

    List<ClassRelation> findByClassEntity_Id(UUID classId);

    List<ClassRelation> findByTargetClassEntity_Id(UUID targetClassId);
}
