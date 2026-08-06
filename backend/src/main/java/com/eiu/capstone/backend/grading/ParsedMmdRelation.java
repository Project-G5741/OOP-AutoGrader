package com.eiu.capstone.backend.grading;

public class ParsedMmdRelation {
    /** Class on the non-target end (UI "from"). */
    public String sourceClassName;
    /** Class on the symbol side (UI "to"). */
    public String targetClassName;
    /** Canonical relation kind: inheritance, composition, aggregation, association, bidirectional_association, link, dependency, realization. */
    public String relationType;
}
