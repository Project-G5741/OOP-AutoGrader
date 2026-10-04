package com.eiu.capstone.backend.grading;

public class ParsedMmdRelation {
    /** Class on the non-target end (UI "from"). */
    public String sourceClassName;
    /** Class on the symbol side (UI "to"). */
    public String targetClassName;
    /**
     * Canonical relation kind: inheritance, composition, aggregation, association,
     * bidirectional_association, bidirectional_inheritance, link, dashed_link, dependency, realization.
     */
    public String relationType;
    /** Optional cardinality on the source side; parsed but not graded in v1. */
    public String sourceCardinality;
    /** Optional cardinality on the target side; parsed but not graded in v1. */
    public String targetCardinality;
}
