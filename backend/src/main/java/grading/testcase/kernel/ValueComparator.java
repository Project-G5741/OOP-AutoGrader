package com.eiu.capstone.backend.grading.testcase.kernel;

import java.util.Arrays;
import java.util.Objects;

import com.eiu.capstone.backend.model.ComparisonMode;

public final class ValueComparator {

    private ValueComparator() {}

    public static boolean matches(Object actual, Object expected, ComparisonMode mode) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        if (actual instanceof Number actualNumber && expected instanceof Number expectedNumber) {
            return compareNumbers(actualNumber, expectedNumber, mode);
        }
        if (actual instanceof String actualText && expected instanceof String expectedText) {
            return compareText(actualText, expectedText, mode);
        }
        if (actual.getClass().isArray() && expected.getClass().isArray()) {
            return Arrays.deepEquals(wrapArray(actual), wrapArray(expected));
        }
        return Objects.equals(actual, expected);
    }

    private static boolean compareNumbers(Number actual, Number expected, ComparisonMode mode) {
        if (mode == ComparisonMode.VALUE_ONLY) {
            return Double.compare(actual.doubleValue(), expected.doubleValue()) == 0;
        }
        if (mode == ComparisonMode.TRIMMED || mode == ComparisonMode.NORMALIZED_WHITESPACE) {
            return Double.compare(actual.doubleValue(), expected.doubleValue()) == 0;
        }
        return numericExact(actual, expected);
    }

    /** Integral types match by long value; floating types by double; never int vs double. */
    private static boolean numericExact(Number actual, Number expected) {
        boolean actualFloating = isFloatingPoint(actual);
        boolean expectedFloating = isFloatingPoint(expected);
        if (actualFloating != expectedFloating) {
            return false;
        }
        if (actualFloating) {
            return Double.compare(actual.doubleValue(), expected.doubleValue()) == 0;
        }
        return actual.longValue() == expected.longValue();
    }

    private static boolean isFloatingPoint(Number value) {
        return value instanceof Float || value instanceof Double;
    }

    private static boolean compareText(String actual, String expected, ComparisonMode mode) {
        return switch (mode) {
            case EXACT, VALUE_ONLY -> actual.equals(expected);
            case TRIMMED -> actual.trim().equals(expected.trim());
            case NORMALIZED_WHITESPACE -> normalizeWhitespace(actual).equals(normalizeWhitespace(expected));
        };
    }

    private static String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private static Object[] wrapArray(Object array) {
        if (array instanceof int[] values) {
            return Arrays.stream(values).boxed().toArray();
        }
        if (array instanceof long[] values) {
            return Arrays.stream(values).boxed().toArray();
        }
        if (array instanceof double[] values) {
            return Arrays.stream(values).boxed().toArray();
        }
        if (array instanceof float[] values) {
            Float[] boxed = new Float[values.length];
            for (int i = 0; i < values.length; i++) {
                boxed[i] = values[i];
            }
            return boxed;
        }
        if (array instanceof boolean[] values) {
            Boolean[] boxed = new Boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                boxed[i] = values[i];
            }
            return boxed;
        }
        if (array instanceof byte[] values) {
            Byte[] boxed = new Byte[values.length];
            for (int i = 0; i < values.length; i++) {
                boxed[i] = values[i];
            }
            return boxed;
        }
        if (array instanceof short[] values) {
            Short[] boxed = new Short[values.length];
            for (int i = 0; i < values.length; i++) {
                boxed[i] = values[i];
            }
            return boxed;
        }
        if (array instanceof char[] values) {
            Character[] boxed = new Character[values.length];
            for (int i = 0; i < values.length; i++) {
                boxed[i] = values[i];
            }
            return boxed;
        }
        return (Object[]) array;
    }
}
