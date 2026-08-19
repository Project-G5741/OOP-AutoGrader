package com.eiu.capstone.backend.grading.mmd.ast;

import java.util.List;

public record MmdClassNode(
        String name,
        String displayLabel,
        List<String> bodyLines,
        int line) implements MmdTopLevelNode {}
