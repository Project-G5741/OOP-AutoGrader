package com.eiu.capstone.backend.grading.mmd.ast;

public record MmdStandaloneStereotypeNode(String stereotype, String className, int line)
        implements MmdTopLevelNode {}
