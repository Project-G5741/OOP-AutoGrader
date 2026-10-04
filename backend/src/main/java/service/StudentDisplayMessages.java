package com.eiu.capstone.backend.service;

/** Fixed student-facing labels — must not echo rubric expected values. */
public final class StudentDisplayMessages {

    public static final String PLACEHOLDER = "—";
    public static final String REQUIRED_DIAGRAM_ELEMENT = "Required diagram element";
    public static final String REQUIRED_RELATIONSHIP = "Required relationship";

    public static final String MISSING_VARIABLE = "Missing variable name or datatype";
    public static final String WRONG_VARIABLE = "Wrong variable name or datatype";
    public static final String MISSING_METHOD = "Missing method name or return type";
    public static final String WRONG_METHOD = "Wrong method name or return type";
    public static final String MISSING_CONSTRUCTOR = "Missing constructor name or parameters";
    public static final String WRONG_CONSTRUCTOR = "Wrong constructor name or parameters";

    public static final String WRONG_SCOPE_OR_MODIFIER = "Missing or wrong scope or modifier";
    public static final String RELATIONSHIP_MISMATCH = "Relationship mismatch";
    public static final String CLASS_MISSING_FROM_DIAGRAM = "Class missing from diagram";
    public static final String MISSING_MMD_FILE = "Missing MMD file";

    public static final String GENERIC_CLASS_TYPE = "CLASS";

    private StudentDisplayMessages() {
    }

    public static String mmdMissingLabel(String attributeType) {
        return switch (attributeType) {
            case "field" -> MISSING_VARIABLE;
            case "constructor" -> MISSING_CONSTRUCTOR;
            case "method" -> MISSING_METHOD;
            default -> REQUIRED_DIAGRAM_ELEMENT;
        };
    }

    public static String mmdWrongLabel(String attributeType) {
        return switch (attributeType) {
            case "field" -> WRONG_VARIABLE;
            case "constructor" -> WRONG_CONSTRUCTOR;
            case "method" -> WRONG_METHOD;
            default -> WRONG_VARIABLE;
        };
    }
}
