package com.eiu.capstone.backend.grading.testcase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonValueCoercer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Object[] coerceParams(String paramsJson, List<String> parameterTypes) {
        JsonNode array = parseArray(paramsJson);
        if (array.size() != parameterTypes.size()) {
            throw new IllegalArgumentException("Parameter count mismatch: expected "
                    + parameterTypes.size() + " but JSON has " + array.size());
        }
        Object[] values = new Object[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            values[i] = coerceValue(array.get(i), parameterTypes.get(i));
        }
        return values;
    }

    public Object coerceExpectedValue(String expectedValueJson, String typeHint) {
        JsonNode node = parseNode(expectedValueJson);
        if (typeHint == null || typeHint.isBlank()) {
            return jsonToObject(node);
        }
        return coerceValue(node, typeHint);
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public JsonNode parseTree(String json) {
        return parseNode(json);
    }

    private JsonNode parseArray(String json) {
        try {
            JsonNode node = objectMapper.readTree(json == null || json.isBlank() ? "[]" : json);
            if (!node.isArray()) {
                throw new IllegalArgumentException("Expected JSON array for params");
            }
            return node;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON params: " + e.getMessage(), e);
        }
    }

    private JsonNode parseNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON value: " + e.getMessage(), e);
        }
    }

    private Object coerceValue(JsonNode node, String typeName) {
        if (node.isNull()) {
            return null;
        }
        if (typeName.endsWith("[]")) {
            return coerceArray(node, typeName.substring(0, typeName.length() - 2));
        }
        return switch (typeName) {
            case "int", "Integer" -> node.isNumber() ? node.intValue() : Integer.parseInt(node.asText());
            case "long", "Long" -> node.isNumber() ? node.longValue() : Long.parseLong(node.asText());
            case "double", "Double" -> node.isNumber() ? node.doubleValue() : Double.parseDouble(node.asText());
            case "float", "Float" -> node.isNumber() ? (float) node.doubleValue() : Float.parseFloat(node.asText());
            case "boolean", "Boolean" -> node.isBoolean() ? node.booleanValue() : Boolean.parseBoolean(node.asText());
            case "byte", "Byte" -> node.isNumber() ? (byte) node.intValue() : Byte.parseByte(node.asText());
            case "short", "Short" -> node.isNumber() ? (short) node.intValue() : Short.parseShort(node.asText());
            case "char", "Character" -> node.asText().isEmpty() ? '\0' : node.asText().charAt(0);
            case "String" -> node.asText();
            default -> throw new IllegalArgumentException("Unsupported type in v1: " + typeName);
        };
    }

    private Object coerceArray(JsonNode node, String elementType) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("Expected JSON array for type " + elementType + "[]");
        }
        List<Object> elements = new ArrayList<>();
        for (JsonNode child : node) {
            elements.add(coerceValue(child, elementType));
        }
        return switch (elementType) {
            case "int", "Integer" -> elements.stream().mapToInt(v -> (Integer) v).toArray();
            case "long", "Long" -> elements.stream().mapToLong(v -> (Long) v).toArray();
            case "double", "Double" -> elements.stream().mapToDouble(v -> (Double) v).toArray();
            case "float", "Float" -> {
                float[] arr = new float[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    arr[i] = (Float) elements.get(i);
                }
                yield arr;
            }
            case "boolean", "Boolean" -> {
                boolean[] arr = new boolean[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    arr[i] = (Boolean) elements.get(i);
                }
                yield arr;
            }
            case "byte", "Byte" -> {
                byte[] arr = new byte[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    arr[i] = (Byte) elements.get(i);
                }
                yield arr;
            }
            case "short", "Short" -> {
                short[] arr = new short[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    arr[i] = (Short) elements.get(i);
                }
                yield arr;
            }
            case "char", "Character" -> {
                char[] arr = new char[elements.size()];
                for (int i = 0; i < elements.size(); i++) {
                    arr[i] = (Character) elements.get(i);
                }
                yield arr;
            }
            case "String" -> elements.toArray(String[]::new);
            default -> throw new IllegalArgumentException("Unsupported array element type: " + elementType);
        };
    }

    private Object jsonToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.intValue();
        }
        if (node.isFloatingPointNumber()) {
            return node.doubleValue();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode child : node) {
                values.add(jsonToObject(child));
            }
            return values;
        }
        if (node.isObject() && node.has("type")) {
            return node.get("type").asText();
        }
        return node.toString();
    }
}
