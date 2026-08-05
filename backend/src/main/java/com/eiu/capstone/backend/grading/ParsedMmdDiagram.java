package com.eiu.capstone.backend.grading;

import java.util.List;

public class ParsedMmdDiagram {
    public List<ParsedMmdClass> classes;
    public List<ParsedMmdRelation> relations;

    public ParsedMmdDiagram(List<ParsedMmdClass> classes, List<ParsedMmdRelation> relations) {
        this.classes = classes;
        this.relations = relations;
    }
}
