package com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.model.ComparisonMode;

public final class ValueComparator {

    private ValueComparator() {}

    public static boolean matches(Object actual, Object expected, ComparisonMode mode) {
        return com.eiu.capstone.backend.grading.testcase.kernel.ValueComparator.matches(actual, expected, mode);
    }
}
