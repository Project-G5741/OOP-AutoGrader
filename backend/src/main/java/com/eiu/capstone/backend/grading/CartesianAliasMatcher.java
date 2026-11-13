package com.eiu.capstone.backend.grading;

import java.util.List;

import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.ParameterRubric;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;

/**
 * Shared independent name/type alias matching for Declaration, MMD, and snapshots.
 */
public final class CartesianAliasMatcher {

    private CartesianAliasMatcher() {}

    public static boolean nameMatches(String actual, List<String> expectedAliases) {
        return expectedAliases.stream()
                .anyMatch(expected -> PartialCreditEvaluator.matches(expected, actual).get(0));
    }

    public static boolean declarationTypeMatches(String actual, List<String> expectedAliases) {
        return expectedAliases.stream()
                .anyMatch(expected -> PartialCreditEvaluator.matchesWhenSpecified(expected, actual));
    }

    public static boolean mmdTypeMatches(String actual, List<String> expectedAliases) {
        return expectedAliases.stream()
                .anyMatch(expected -> expected == null || expected.isBlank()
                        || MmdTypeEquivalence.typesMatch(expected, actual));
    }

    public static boolean declarationParameterTypesMatch(
            List<String> actualTypes, List<ParameterRubric> expectedParameters) {
        return parameterTypesMatch(actualTypes, expectedParameters, false);
    }

    public static boolean mmdParameterTypesMatch(
            List<String> actualTypes, List<ParameterRubric> expectedParameters) {
        return parameterTypesMatch(actualTypes, expectedParameters, true);
    }

    /**
     * Reflection parameter names are optional bytecode metadata. Enforce aliases for
     * real names, but skip the name check for absent names and javac-style argN names.
     */
    public static boolean declarationParametersMatch(
            List<String> actualNames,
            List<String> actualTypes,
            List<ParameterRubric> expectedParameters) {
        return parameterTypesMatch(actualTypes, expectedParameters, false)
                && parameterNamesMatch(actualNames, expectedParameters, true);
    }

    public static boolean mmdParametersMatch(
            List<String> actualNames,
            List<String> actualTypes,
            List<ParameterRubric> expectedParameters) {
        return parameterTypesMatch(actualTypes, expectedParameters, true)
                && parameterNamesMatch(actualNames, expectedParameters, false);
    }

    public static ParsedField findBestDeclarationField(
            List<ParsedField> candidates, FieldRubric expected, boolean enumClass) {
        return findBestField(
                candidates, expected, field -> declarationFieldMatches(field, expected, enumClass));
    }

    public static boolean declarationFieldMatches(
            ParsedField actual, FieldRubric expected, boolean enumClass) {
        return enumClass
                || (PartialCreditEvaluator.matchesWhenSpecified(expected.scope(), actual.scope)
                        && declarationTypeMatches(actual.dataType, expected.dataTypes())
                        && expected.isStatic() == actual.isStatic
                        && expected.isFinal() == actual.isFinal);
    }

    public static ParsedField findBestMmdField(
            List<ParsedField> candidates, FieldRubric expected, boolean enumClass) {
        return findBestField(candidates, expected, field -> mmdFieldMatches(field, expected, enumClass));
    }

    public static boolean mmdFieldMatches(
            ParsedField actual, FieldRubric expected, boolean enumClass) {
        boolean nameOnly = enumClass
                || (expected.dataTypes().stream().noneMatch(CartesianAliasMatcher::specified)
                        && !specified(expected.scope()));
        return nameOnly
                || (!actual.invalidDeclarationFormat
                        && PartialCreditEvaluator.matchesWhenSpecified(expected.scope(), actual.scope)
                        && (expected.dataTypes().stream().anyMatch(CartesianAliasMatcher::specified)
                                ? mmdTypeMatches(actual.dataType, expected.dataTypes())
                                : true)
                        && (!expected.isStatic() || actual.isStatic));
    }

    private static boolean parameterTypesMatch(
            List<String> actualTypes,
            List<ParameterRubric> expectedParameters,
            boolean mmd) {
        if (actualTypes == null || actualTypes.size() != expectedParameters.size()) {
            return false;
        }
        for (int i = 0; i < actualTypes.size(); i++) {
            boolean matches = mmd
                    ? mmdTypeMatches(actualTypes.get(i), expectedParameters.get(i).dataTypes())
                    : declarationTypeMatches(actualTypes.get(i), expectedParameters.get(i).dataTypes());
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private static boolean parameterNamesMatch(
            List<String> actualNames,
            List<ParameterRubric> expectedParameters,
            boolean allowUnavailable) {
        if (expectedParameters.isEmpty()) {
            return actualNames == null || actualNames.isEmpty();
        }
        if (actualNames == null || actualNames.size() != expectedParameters.size()) {
            return allowUnavailable;
        }
        for (int i = 0; i < actualNames.size(); i++) {
            String actualName = actualNames.get(i);
            if (allowUnavailable && unavailableReflectionName(actualName, i)) {
                continue;
            }
            if (actualName == null
                    || !nameMatches(actualName, expectedParameters.get(i).names())) {
                return false;
            }
        }
        return true;
    }

    private static boolean unavailableReflectionName(String name, int position) {
        return name == null || name.isBlank() || name.equals("arg" + position);
    }

    private static ParsedField findBestField(
            List<ParsedField> candidates,
            FieldRubric expected,
            java.util.function.Predicate<ParsedField> completeMatch) {
        ParsedField firstNameMatch = null;
        for (ParsedField candidate : candidates) {
            if (!nameMatches(candidate.name, expected.names())) {
                continue;
            }
            if (firstNameMatch == null) {
                firstNameMatch = candidate;
            }
            if (completeMatch.test(candidate)) {
                return candidate;
            }
        }
        return firstNameMatch;
    }

    private static boolean specified(String value) {
        return value != null && !value.isBlank();
    }
}
