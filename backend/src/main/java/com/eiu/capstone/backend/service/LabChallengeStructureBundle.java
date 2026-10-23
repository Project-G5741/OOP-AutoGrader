package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ClassRelation;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;

/**
 * Rubric structure for one or more challenges, loaded in a small number of batched queries.
 * Used by upload-time {@code lab_result} assembly to avoid per-challenge N+1 reads.
 */
public record LabChallengeStructureBundle(
        Map<Integer, String> masterData,
        Map<UUID, List<ClassEntity>> classesByChallengeId,
        Map<UUID, List<Field>> fieldsByClassId,
        Map<UUID, List<Method>> methodsByClassId,
        Map<UUID, List<Constructor>> constructorsByClassId,
        Map<UUID, List<Parameter>> paramsByConstructorId,
        Map<UUID, List<Parameter>> paramsByMethodId,
        Map<UUID, List<ClassRelation>> relationsBySourceClassId) {

    public List<ClassEntity> classesForChallenge(UUID challengeId) {
        return classesByChallengeId.getOrDefault(challengeId, List.of());
    }
}
