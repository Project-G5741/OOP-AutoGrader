package com.eiu.capstone.backend.grading;

import java.util.List;

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
}
