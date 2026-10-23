package com.eiu.capstone.backend.grading.rubric;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChallengeRubric(
        UUID challengeId,
        int challengeNumber,
        String name,
        List<ClassRubric> classes,
        List<RelationRubric> relations,
        List<TestcaseRubric> testcases,
        boolean hasMmd,
        boolean hasClass,
        int weight,
        int classWeight,
        int mmdWeight,
        int testcaseWeight) {

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations) {
        this(challengeId, challengeNumber, name, classes, relations, List.of(), true, true, 1, 1, 1, 1);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, true, true, 1, 1, 1, 1);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases,
                           boolean hasMmd) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, hasMmd, true, 1, 1, 1, 1);
    }

    public ChallengeRubric(UUID challengeId,
                           int challengeNumber,
                           String name,
                           List<ClassRubric> classes,
                           List<RelationRubric> relations,
                           List<TestcaseRubric> testcases,
                           boolean hasMmd,
                           boolean hasClass) {
        this(challengeId, challengeNumber, name, classes, relations, testcases, hasMmd, hasClass, 1, 1, 1, 1);
    }

    /** Legacy full ctor (pre-{@code hasClass}); Declaration defaults on. */
    public ChallengeRubric(UUID challengeId,
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
        this(challengeId, challengeNumber, name, classes, relations, testcases, hasMmd, true,
                weight, classWeight, mmdWeight, testcaseWeight);
    }
}
