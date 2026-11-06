package com.eiu.capstone.backend.grading;

import java.util.List;

public class ParsedMethod {
    public String name;
    public String returnType;
    public String scope;
    public boolean isStatic;
    public boolean isAbstract;
    public boolean isFinal;
    public List<String> parameterTypes;
    public List<String> parameterNames;
    /** True when any parameter was not {@code name: datatype} (spacing around {@code :} ignored). */
    public boolean invalidDeclarationFormat;
}