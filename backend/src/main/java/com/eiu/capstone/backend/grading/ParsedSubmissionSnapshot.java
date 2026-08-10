package com.eiu.capstone.backend.grading;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Per-submission display capture for Class/MMD result tabs.
 * Keyed by challenge UUID string at the store root.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedSubmissionSnapshot {

    public Map<String, ChallengeSnapshot> challenges = new HashMap<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChallengeSnapshot {
        public ClassSnapshot classSnapshot = new ClassSnapshot();
        public MmdSnapshot mmdSnapshot = new MmdSnapshot();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassSnapshot {
        public Map<String, ClassFieldEntry> fields = new HashMap<>();
        public Map<String, ClassMethodEntry> methods = new HashMap<>();
        public Map<String, ClassConstructorEntry> constructors = new HashMap<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassFieldEntry {
        public String name;
        public String scope;
        public String dataType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassMethodEntry {
        public String name;
        public String scope;
        public String returnType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassConstructorEntry {
        public String name;
        public String scope;
        public String params;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MmdSnapshot {
        /** Rubric class entity id -> stereotype display e.g. {@code <<interface>>}. */
        public Map<String, String> stereotypes = new HashMap<>();
        /** Rubric element id -> attribute display line. */
        public Map<String, String> attributes = new HashMap<>();
        public Map<String, MmdRelationEntry> relations = new HashMap<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MmdRelationEntry {
        public String from;
        public String to;
        public String relType;
    }
}
