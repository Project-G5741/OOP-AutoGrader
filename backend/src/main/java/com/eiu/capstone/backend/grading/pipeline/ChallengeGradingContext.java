package com.eiu.capstone.backend.grading.pipeline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedClassIndex;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;

public record ChallengeGradingContext(
        ChallengeRubric challengeRubric,
        Path classesDir,
        String compileError,
        List<ParsedClass> parsedClasses,
        Map<String, ParsedClass> parsedByName,
        Map<String, ParsedClass> parsedByQualifiedName,
        Set<String> failedClassNames) {

    public static ChallengeGradingContext of(ChallengeRubric rubric,
                                             Path classesDir,
                                             String compileError,
                                             List<ParsedClass> parsedClasses) {
        ParsedClassIndex index = ParsedClassIndex.of(parsedClasses);
        return new ChallengeGradingContext(
                rubric,
                classesDir,
                compileError,
                parsedClasses,
                index.byName(),
                index.byQualifiedName(),
                Set.of());
    }

    public ParsedClass resolve(ClassRubric expectedClass) {
        if (expectedClass.isNested()) {
            return parsedByQualifiedName.get(expectedClass.qualifiedName());
        }
        return parsedByName.get(expectedClass.name());
    }
}
