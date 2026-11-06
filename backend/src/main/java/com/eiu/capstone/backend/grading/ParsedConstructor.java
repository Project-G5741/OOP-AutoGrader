package com.eiu.capstone.backend.grading;

import java.util.List;

public class ParsedConstructor {
    public String scope;
    public List<String> parameterTypes;
    public List<String> parameterNames;
    /** True when any parameter was not {@code name: datatype} (spacing around {@code :} ignored). */
    public boolean invalidDeclarationFormat;
}