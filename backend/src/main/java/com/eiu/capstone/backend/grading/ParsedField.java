package com.eiu.capstone.backend.grading;

public class ParsedField {
    public String name;
    public String dataType;
    public String scope;
    public boolean isStatic;
    public boolean isFinal;
    /** True for a Java enum constant. Those rows have no lecturer-authored type or scope. */
    public boolean isEnumConstant;
    /** True when the diagram line was not {@code name: datatype} (spacing around {@code :} ignored). */
    public boolean invalidDeclarationFormat;
}