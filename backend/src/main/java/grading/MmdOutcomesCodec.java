package com.eiu.capstone.backend.grading;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Versioned JSON document for {@code submission_challenge_result.mmd_outcomes}.
 * Written at grade time; GET {@code /mmd} reconstructs {@link MmdGradingOutcome}.
 */
public final class MmdOutcomesCodec {

    public static final int VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MmdOutcomesCodec() {}

    public static String toJson(MmdGradingOutcome outcome) {
        if (outcome == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(Document.from(outcome));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize MMD outcomes", e);
        }
    }

    public static MmdGradingOutcome fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Document doc = MAPPER.readValue(json, Document.class);
            return doc == null ? null : doc.toOutcome();
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            int v,
            Map<String, ClassShell> classes,
            Map<String, Boolean> fields,
            Map<String, Boolean> methods,
            Map<String, Boolean> constructors,
            Map<String, Boolean> relations) {

        static Document from(MmdGradingOutcome outcome) {
            Map<String, ClassShell> classes = new LinkedHashMap<>();
            Map<String, Boolean> fields = new LinkedHashMap<>();
            Map<String, Boolean> methods = new LinkedHashMap<>();
            Map<String, Boolean> constructors = new LinkedHashMap<>();
            Map<String, Boolean> relations = new LinkedHashMap<>();

            for (UUID id : outcome.classIds()) {
                classes.put(id.toString(), new ClassShell(
                        outcome.isClassPresent(id),
                        outcome.isClassCorrect(id)));
            }
            for (UUID id : outcome.fieldIds()) {
                fields.put(id.toString(), outcome.isFieldCorrect(id));
            }
            for (UUID id : outcome.methodIds()) {
                methods.put(id.toString(), outcome.isMethodCorrect(id));
            }
            for (UUID id : outcome.constructorIds()) {
                constructors.put(id.toString(), outcome.isConstructorCorrect(id));
            }
            for (UUID id : outcome.relationIds()) {
                relations.put(id.toString(), outcome.isRelationCorrect(id));
            }
            return new Document(VERSION, classes, fields, methods, constructors, relations);
        }

        MmdGradingOutcome toOutcome() {
            MmdGradingOutcome outcome = new MmdGradingOutcome();
            if (classes != null) {
                for (Map.Entry<String, ClassShell> e : classes.entrySet()) {
                    UUID id = parseUuid(e.getKey());
                    if (id == null || e.getValue() == null) {
                        continue;
                    }
                    outcome.setClassPresent(id, e.getValue().present());
                    outcome.setClass(id, e.getValue().typeOk());
                }
            }
            if (fields != null) {
                fields.forEach((key, value) -> {
                    UUID id = parseUuid(key);
                    if (id != null && value != null) {
                        outcome.setField(id, value);
                    }
                });
            }
            if (methods != null) {
                methods.forEach((key, value) -> {
                    UUID id = parseUuid(key);
                    if (id != null && value != null) {
                        outcome.setMethod(id, value);
                    }
                });
            }
            if (constructors != null) {
                constructors.forEach((key, value) -> {
                    UUID id = parseUuid(key);
                    if (id != null && value != null) {
                        outcome.setConstructor(id, value);
                    }
                });
            }
            if (relations != null) {
                relations.forEach((key, value) -> {
                    UUID id = parseUuid(key);
                    if (id != null && value != null) {
                        outcome.setRelation(id, value);
                    }
                });
            }
            return outcome;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClassShell(boolean present, boolean typeOk) {}

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
