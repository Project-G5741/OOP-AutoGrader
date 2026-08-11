package com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.ComparisonMode;

class ValueComparatorTest {

    @Test
    void matchesNumericValuesAcrossNumberTypes() {
        assertTrue(ValueComparator.matches(5, 5.0, ComparisonMode.EXACT));
        assertTrue(ValueComparator.matches(Integer.valueOf(0), Long.valueOf(0L), ComparisonMode.EXACT));
        assertFalse(ValueComparator.matches(1, 2, ComparisonMode.EXACT));
    }

    @Test
    void matchesTrimmedText() {
        assertTrue(ValueComparator.matches(" hello ", "hello", ComparisonMode.TRIMMED));
        assertFalse(ValueComparator.matches("hello", "world", ComparisonMode.TRIMMED));
    }
}
