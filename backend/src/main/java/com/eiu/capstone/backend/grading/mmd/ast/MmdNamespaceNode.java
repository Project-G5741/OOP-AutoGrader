package com.eiu.capstone.backend.grading.mmd.ast;

import java.util.List;

public record MmdNamespaceNode(String name, List<MmdTopLevelNode> children, int line) implements MmdTopLevelNode {}
