package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

public record ChallengeRubric(
        UUID challengeId,
        int challengeNumber,
        String name,
        List<ClassRubric> classes,
        List<RelationRubric> relations,
        List<TestcaseRubric> testcases,
        boolean hasMmd,
        int weight,
        int classWeight,
        int mmdWeight,
        int testcaseWeight) {

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations) {
        this(challengeId, challengeNumber, name, classes, relations, List.of(), true, 1, 1, 1, 1);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, true, 1, 1, 1, 1);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases,
                           boolean hasMmd) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, hasMmd, 1, 1, 1, 1);
    }
}
