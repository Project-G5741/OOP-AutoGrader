package com.eiu.capstone.backend.grading.pipeline;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedClassIndex;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;

public record ChallengeGradingContext(
        ChallengeRubric challengeRubric,
        Path classesDir,
        String compileError,
        List<ParsedClass> parsedClasses,
        Map<String, ParsedClass> parsedByName,
        Map<String, ParsedClass> parsedByQualifiedName,
        Set<String> failedClassNames,
        Map<String, String> compileErrorsByClassName,
        WorkerSessionHandle workerSession) {

    public static ChallengeGradingContext of(ChallengeRubric rubric,
                                             Path classesDir,
                                             String compileError,
                                             List<ParsedClass> parsedClasses) {
        return of(rubric, classesDir, compileError, parsedClasses, Set.of(), Map.of());
    }

    public static ChallengeGradingContext of(ChallengeRubric rubric,
                                             Path classesDir,
                                             String compileError,
                                             List<ParsedClass> parsedClasses,
                                             Set<String> failedClassNames,
                                             Map<String, String> compileErrorsByClassName) {
        ParsedClassIndex index = ParsedClassIndex.of(parsedClasses);
        return of(rubric, classesDir, compileError, parsedClasses, failedClassNames, compileErrorsByClassName, null);
    }

    public static ChallengeGradingContext of(ChallengeRubric rubric,
                                             Path classesDir,
                                             String compileError,
                                             List<ParsedClass> parsedClasses,
                                             Set<String> failedClassNames,
                                             Map<String, String> compileErrorsByClassName,
                                             WorkerSessionHandle workerSession) {
        ParsedClassIndex index = ParsedClassIndex.of(parsedClasses);
        return new ChallengeGradingContext(
                rubric,
                classesDir,
                compileError,
                parsedClasses,
                index.byName(),
                index.byQualifiedName(),
                failedClassNames == null ? Set.of() : Set.copyOf(failedClassNames),
                compileErrorsByClassName == null ? Map.of() : Map.copyOf(compileErrorsByClassName),
                workerSession);
    }

    public ParsedClass resolve(ClassRubric expectedClass) {
        if (expectedClass.isNested()) {
            return parsedByQualifiedName.get(expectedClass.qualifiedName());
        }
        return parsedByName.get(expectedClass.name());
    }
}
