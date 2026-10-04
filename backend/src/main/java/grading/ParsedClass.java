package com.eiu.capstone.backend.grading;

import java.util.List;

public class ParsedClass {
    public String simpleName;
    public String outerSimpleName;
    public String scope;        
    public String declaringType;
    public boolean isAbstract;
    public boolean isStatic;
    /** Immediate superclass simple name, omitting {@code Object}. */
    public String superclassSimpleName;
    /** Declared interface simple names from the compiled type. */
    public List<String> interfaceSimpleNames;
    public List<ParsedField> fields;
    public List<ParsedMethod> methods;
    public List<ParsedConstructor> constructors;
}