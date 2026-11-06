package com.eiu.capstone.backend.grading;

import java.util.ArrayList;
import java.util.List;

public class ParsedMmdClass {
    public String name;
    /** Enum, Interface, or Class — from stereotype; null means plain Class. */
    public String stereotypeType;
    public List<ParsedField> fields = new ArrayList<>();
    public List<ParsedMethod> methods = new ArrayList<>();
    public List<ParsedConstructor> constructors = new ArrayList<>();
}
