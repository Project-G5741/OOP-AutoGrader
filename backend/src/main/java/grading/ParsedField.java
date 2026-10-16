package com.eiu.capstone.backend.grading;

public class ParsedField {
    public String name;
    public String dataType;
    public String scope;
    /** True when the diagram line was not {@code name: datatype} (spacing around {@code :} ignored). */
    public boolean invalidDeclarationFormat;
}