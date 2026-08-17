package com.eiu.capstone.backend.grading.pipeline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;

class ClassReflectionGraderTest {

    private final ClassReflectionGrader grader = new ClassReflectionGrader();

    @Test
    void explicitPrivateNoArgConstructorPassesWhenRubricIsNotDefault() {
        UUID ctorId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Logger",
                List.of(new ClassRubric(
                        classId,
                        "Logger",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(),
                        List.of(new ConstructorRubric(ctorId, "private", false, List.of())))),
                List.of(),
                List.of());

        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = "Logger";
        parsed.scope = "public";
        parsed.declaringType = "CLASS";
        parsed.isAbstract = false;
        parsed.fields = List.of();
        parsed.methods = List.of();
        ParsedConstructor ctor = new ParsedConstructor();
        ctor.scope = "private";
        ctor.parameterTypes = List.of();
        parsed.constructors = List.of(ctor);

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertTrue(result.constructors().stream()
                .filter(entry -> entry.constructorId().equals(ctorId))
                .findFirst()
                .orElseThrow()
                .correct());
    }
}
