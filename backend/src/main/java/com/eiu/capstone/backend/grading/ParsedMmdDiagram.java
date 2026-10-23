package com.eiu.capstone.backend.grading;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ParsedMmdDiagram {
    public List<ParsedMmdClass> classes;
    public List<ParsedMmdRelation> relations;
    /** Simple and qualified class names for comparison lookup (e.g. {@code Employee}, {@code Company.Employee}). */
    public Map<String, ParsedMmdClass> classByName;

    public ParsedMmdDiagram(List<ParsedMmdClass> classes, List<ParsedMmdRelation> relations) {
        this(classes, relations, Map.of());
    }

    public ParsedMmdDiagram(
            List<ParsedMmdClass> classes,
            List<ParsedMmdRelation> relations,
            Map<String, ParsedMmdClass> classByName) {
        this.classes = classes;
        this.relations = relations;
        this.classByName = classByName == null ? Map.of() : Collections.unmodifiableMap(classByName);
    }
}
