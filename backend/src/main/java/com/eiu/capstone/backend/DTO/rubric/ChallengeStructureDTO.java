package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record ChallengeStructureDTO(
        UUID id,
        String name,
        Integer challengeNumber,
        List<ClassStructureDTO> classes,
        List<RelationStructureDTO> relations,
        boolean hasMmd,
        boolean hasClass,
        int weight,
        int classWeight,
        int mmdWeight,
        int testcaseWeight) {

    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations) {
        this(id, name, challengeNumber, classes, relations, true, true, 1, 1, 1, 1);
    }

    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations,
                                 boolean hasMmd) {
        this(id, name, challengeNumber, classes, relations, hasMmd, true, 1, 1, 1, 1);
    }

    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations,
                                 boolean hasMmd,
                                 boolean hasClass) {
        this(id, name, challengeNumber, classes, relations, hasMmd, hasClass, 1, 1, 1, 1);
    }

    /** Legacy full ctor (pre-{@code hasClass}); Declaration defaults on. */
    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations,
                                 boolean hasMmd,
                                 int weight,
                                 int classWeight,
                                 int mmdWeight,
                                 int testcaseWeight) {
        this(id, name, challengeNumber, classes, relations, hasMmd, true,
                weight, classWeight, mmdWeight, testcaseWeight);
    }
}
