package com.eiu.capstone.backend.grading.mmd.ast;

public record MmdRelationNode(
        String leftClass,
        String arrow,
        String rightClass,
        String label,
        String leftCardinality,
        String rightCardinality,
        int line) implements MmdTopLevelNode {

    public MmdRelationNode(
            String leftClass,
            String arrow,
            String rightClass,
            String label,
            int line) {
        this(leftClass, arrow, rightClass, label, null, null, line);
    }
}
