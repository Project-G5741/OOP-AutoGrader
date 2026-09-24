package unit.com.eiu.capstone.backend.grading.testcase;

import com.eiu.capstone.backend.grading.testcase.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.ComparisonMode;

class ValueComparatorTest {

    @Test
    void matchesNumericValuesAcrossNumberTypesWhenNumericValueMode() {
        assertTrue(ValueComparator.matches(5, 5.0, ComparisonMode.VALUE_ONLY));
        assertTrue(ValueComparator.matches(Integer.valueOf(0), Long.valueOf(0L), ComparisonMode.VALUE_ONLY));
        assertFalse(ValueComparator.matches(1, 2, ComparisonMode.VALUE_ONLY));
    }

    @Test
    void exactModeDistinguishesIntegralFromFloating() {
        assertFalse(ValueComparator.matches(10, 10.0, ComparisonMode.EXACT));
        assertTrue(ValueComparator.matches(10, 10, ComparisonMode.EXACT));
        assertTrue(ValueComparator.matches(10L, 10, ComparisonMode.EXACT));
        assertTrue(ValueComparator.matches(10.0f, 10.0, ComparisonMode.EXACT));
    }

    @Test
    void matchesTrimmedText() {
        assertTrue(ValueComparator.matches(" hello ", "hello", ComparisonMode.TRIMMED));
        assertFalse(ValueComparator.matches("hello", "world", ComparisonMode.TRIMMED));
    }
}
