package com.eiu.capstone.backend.DTO.rubric;

import java.util.List;
import java.util.UUID;

public record ChallengeStructureDTO(
        UUID id,
        String name,
        Integer challengeNumber,
        List<ClassStructureDTO> classes,
        List<RelationStructureDTO> relations) {}
