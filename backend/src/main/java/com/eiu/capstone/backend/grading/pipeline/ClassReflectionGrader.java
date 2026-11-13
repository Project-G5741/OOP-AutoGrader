package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.CartesianAliasMatcher;
import com.eiu.capstone.backend.grading.EnumDeclaringTypes;
import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.ParsedField;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;

/**
 * Grades compiled student classes against the full rubric for the .class pillar.
 * Class shells and members are all-or-nothing: every graded attribute must match.
 */
@Component
public class ClassReflectionGrader {

    public ClassPillarResult grade(ChallengeGradingContext context) {
        List<WeightedAccuracy> weighted = new ArrayList<>();
        List<PendingFieldResult> fields = new ArrayList<>();
        List<PendingMethodResult> methods = new ArrayList<>();
        List<PendingConstructorResult> constructors = new ArrayList<>();

        for (ClassRubric expectedClass : context.challengeRubric().classes()) {
            ParsedClass parsed = classCompileFailed(context, expectedClass) ? null : context.resolve(expectedClass);
            int classWeight = MemberWeightCalculator.configuredWeight(expectedClass.weight());

            if (parsed == null) {
                weighted.add(new WeightedAccuracy(classWeight, 0));
                for (FieldRubric f : expectedClass.fields()) {
                    weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), 0));
                    fields.add(new PendingFieldResult(f.id(), false, false));
                }
                for (MethodRubric m : expectedClass.methods()) {
                    weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), 0));
                    methods.add(new PendingMethodResult(m.id(), false, false));
                }
                for (ConstructorRubric c : expectedClass.constructors()) {
                    weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), 0));
                    constructors.add(new PendingConstructorResult(c.id(), false, false));
                }
                continue;
            }

            List<Boolean> classChecks = new ArrayList<>();
            classChecks.add(PartialCreditEvaluator.matches(expectedClass.scope(), parsed.scope).get(0));
            classChecks.add(PartialCreditEvaluator.matches(expectedClass.declaringType(), parsed.declaringType).get(0));
            classChecks.add(expectedClass.isAbstract() == parsed.isAbstract);
            if (expectedClass.isNested()) {
                classChecks.add(expectedClass.isStatic() == parsed.isStatic);
            }
            classChecks.add(HeritageShellMatcher.heritageMatchesOrSkipped(
                    expectedClass, parsed, context.challengeRubric()));
            double classAccuracy = classChecks.stream().allMatch(Boolean::booleanValue) ? 1.0 : 0.0;
            weighted.add(new WeightedAccuracy(classWeight, classAccuracy));
            boolean shellPassed = classAccuracy >= 1.0;

            for (FieldRubric expectedField : expectedClass.fields()) {
                ParsedField pf = parsed.fields.stream()
                        .filter(field -> CartesianAliasMatcher.nameMatches(field.name, expectedField.names()))
                        .findFirst()
                        .orElse(null);
                double accuracy;
                if (!shellPassed) {
                    accuracy = 0;
                } else if (pf == null) {
                    accuracy = 0;
                } else {
                    boolean allMatch = EnumDeclaringTypes.isEnum(expectedClass.declaringType())
                            || (PartialCreditEvaluator.matchesWhenSpecified(expectedField.scope(), pf.scope)
                                    && CartesianAliasMatcher.declarationTypeMatches(
                                            pf.dataType, expectedField.dataTypes())
                                    && expectedField.isStatic() == pf.isStatic
                                    && expectedField.isFinal() == pf.isFinal);
                    accuracy = PartialCreditEvaluator.binaryAccuracy(allMatch);
                }
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), accuracy));
                fields.add(new PendingFieldResult(expectedField.id(), shellPassed && accuracy >= 1.0, false));
            }

            for (MethodRubric expectedMethod : expectedClass.methods()) {
                ParsedMethod match = findMatchingMethod(parsed.methods, expectedMethod);
                double accuracy;
                if (!shellPassed) {
                    accuracy = 0;
                } else if (match == null) {
                    accuracy = 0;
                } else {
                    boolean allMatch = PartialCreditEvaluator.matches(expectedMethod.scope(), match.scope).get(0)
                            && CartesianAliasMatcher.declarationTypeMatches(
                                    match.returnType, expectedMethod.returnTypes())
                            && expectedMethod.isStatic() == match.isStatic
                            && expectedMethod.isAbstract() == match.isAbstract
                            && expectedMethod.isFinal() == match.isFinal;
                    accuracy = PartialCreditEvaluator.binaryAccuracy(allMatch);
                }
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), accuracy));
                methods.add(new PendingMethodResult(expectedMethod.id(), shellPassed && accuracy >= 1.0, false));
            }

            for (ConstructorRubric expectedConstructor : expectedClass.constructors()) {
                ParsedConstructor match = findMatchingConstructor(
                        parsed.constructors,
                        expectedConstructor.parameters(),
                        expectedClass.isNested() && !expectedClass.isStatic(),
                        parsed.outerSimpleName);
                double accuracy;
                if (!shellPassed) {
                    accuracy = 0;
                } else if (match == null) {
                    accuracy = 0;
                } else {
                    boolean allMatch = PartialCreditEvaluator.matches(expectedConstructor.scope(), match.scope).get(0)
                            && constructorDefaultMatches(expectedConstructor, match);
                    accuracy = PartialCreditEvaluator.binaryAccuracy(allMatch);
                }
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), accuracy));
                constructors.add(new PendingConstructorResult(
                        expectedConstructor.id(),
                        shellPassed && accuracy >= 1.0,
                        false));
            }
        }

        BigDecimal pillarPct = PillarScoreAggregator.pillarPercentage(weighted);
        return new ClassPillarResult(pillarPct, fields, methods, constructors);
    }

    /**
     * Canonical result when {@code has_class} is false — the grader is never invoked.
     */
    public static ClassPillarResult notApplicable() {
        return new ClassPillarResult(BigDecimal.ZERO, List.of(), List.of(), List.of());
    }

    /**
     * Reflection cannot distinguish an explicit no-arg constructor from a compiler default.
     * Only enforce the default-constructor rubric flag when the rubric expects one (public, no-arg).
     */
    private boolean constructorDefaultMatches(ConstructorRubric expected, ParsedConstructor actual) {
        if (!expected.isDefault()) {
            return true;
        }
        return actual.parameterTypes.isEmpty()
                && equalsIgnoreCase("public", actual.scope);
    }

    private ParsedMethod findMatchingMethod(List<ParsedMethod> candidates, MethodRubric expected) {
        for (ParsedMethod pm : candidates) {
            if (CartesianAliasMatcher.nameMatches(pm.name, expected.names())
                    && CartesianAliasMatcher.declarationParameterTypesMatch(
                            pm.parameterTypes, expected.parameters())) {
                return pm;
            }
        }
        return null;
    }

    private ParsedConstructor findMatchingConstructor(List<ParsedConstructor> candidates,
                                                      List<com.eiu.capstone.backend.grading.rubric.ParameterRubric> expectedParameters,
                                                      boolean stripImplicitOuterParam,
                                                      String outerSimpleName) {
        for (ParsedConstructor pc : candidates) {
            List<String> actualParams = pc.parameterTypes;
            if (stripImplicitOuterParam
                    && outerSimpleName != null
                    && !outerSimpleName.isBlank()
                    && !actualParams.isEmpty()
                    && equalsIgnoreCase(actualParams.get(0), outerSimpleName)) {
                actualParams = actualParams.subList(1, actualParams.size());
            }
            if (CartesianAliasMatcher.declarationParameterTypesMatch(actualParams, expectedParameters)) {
                return pc;
            }
        }
        return null;
    }

    private static boolean classCompileFailed(ChallengeGradingContext context, ClassRubric expectedClass) {
        return context.failedClassNames().contains(expectedClass.name())
                || context.failedClassNames().contains(expectedClass.qualifiedName());
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    public record ClassPillarResult(
            BigDecimal pillarPercentage,
            List<PendingFieldResult> fields,
            List<PendingMethodResult> methods,
            List<PendingConstructorResult> constructors) {}

    public record PendingFieldResult(java.util.UUID fieldId, boolean correct, boolean partial) {}
    public record PendingMethodResult(java.util.UUID methodId, boolean correct, boolean partial) {}
    public record PendingConstructorResult(java.util.UUID constructorId, boolean correct, boolean partial) {}
}
