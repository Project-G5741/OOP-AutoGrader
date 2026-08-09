package com.eiu.capstone.backend.grading.pipeline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;

public record ChallengeGradingContext(
        ChallengeRubric challengeRubric,
        Path classesDir,
        String compileError,
        List<ParsedClass> parsedClasses,
        Map<String, ParsedClass> parsedByName,
        Set<String> failedClassNames) {

    public static ChallengeGradingContext of(ChallengeRubric rubric,
                                             Path classesDir,
                                             String compileError,
                                             List<ParsedClass> parsedClasses) {
        Map<String, ParsedClass> byName = parsedClasses.stream()
                .collect(java.util.stream.Collectors.toMap(pc -> pc.simpleName, pc -> pc, (a, b) -> a));
        Set<String> failed = compileError != null && !compileError.isBlank()
                ? Set.of()
                : Set.of();
        return new ChallengeGradingContext(rubric, classesDir, compileError, parsedClasses, byName, failed);
    }
}
