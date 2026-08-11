package com.eiu.capstone.backend.grading.testcase;

import java.util.List;

public final class JavaTypeResolver {

    private JavaTypeResolver() {}

    public static Class<?> resolve(String typeName) {
        if (typeName.endsWith("[]")) {
            return resolveArrayClass(typeName.substring(0, typeName.length() - 2));
        }
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            case "Integer" -> Integer.class;
            case "Long" -> Long.class;
            case "Double" -> Double.class;
            case "Float" -> Float.class;
            case "Boolean" -> Boolean.class;
            case "Byte" -> Byte.class;
            case "Short" -> Short.class;
            case "Character" -> Character.class;
            case "String" -> String.class;
            default -> throw new IllegalArgumentException("Unsupported type in v1: " + typeName);
        };
    }

    public static Class<?>[] resolveAll(List<String> parameterTypes) {
        Class<?>[] types = new Class<?>[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            types[i] = resolve(parameterTypes.get(i));
        }
        return types;
    }

    private static Class<?> resolveArrayClass(String elementType) {
        return switch (elementType) {
            case "int", "Integer" -> int[].class;
            case "long", "Long" -> long[].class;
            case "double", "Double" -> double[].class;
            case "float", "Float" -> float[].class;
            case "boolean", "Boolean" -> boolean[].class;
            case "byte", "Byte" -> byte[].class;
            case "short", "Short" -> short[].class;
            case "char", "Character" -> char[].class;
            case "String" -> String[].class;
            default -> throw new IllegalArgumentException("Unsupported array type: " + elementType + "[]");
        };
    }
}
