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
        int weight,
        int classWeight,
        int mmdWeight) {

    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations) {
        this(id, name, challengeNumber, classes, relations, true, 1, 1, 1);
    }

    public ChallengeStructureDTO(UUID id,
                                 String name,
                                 Integer challengeNumber,
                                 List<ClassStructureDTO> classes,
                                 List<RelationStructureDTO> relations,
                                 boolean hasMmd) {
        this(id, name, challengeNumber, classes, relations, hasMmd, 1, 1, 1);
    }
}
