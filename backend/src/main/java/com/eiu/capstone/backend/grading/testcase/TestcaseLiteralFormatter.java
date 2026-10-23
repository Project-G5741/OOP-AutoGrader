package com.eiu.capstone.backend.grading.testcase;

import java.util.Arrays;

final class TestcaseLiteralFormatter {

    private TestcaseLiteralFormatter() {}

    static String format(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + text + "\"";
        }
        if (value.getClass().isArray()) {
            return arrayToString(value);
        }
        return String.valueOf(value);
    }

    private static String arrayToString(Object array) {
        if (array instanceof int[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof long[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof double[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof float[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof boolean[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof byte[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof short[] values) {
            return Arrays.toString(values);
        }
        if (array instanceof char[] values) {
            return Arrays.toString(values);
        }
        return Arrays.toString((Object[]) array);
    }
}
