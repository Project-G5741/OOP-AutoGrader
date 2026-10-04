package com.eiu.capstone.backend.grading.mmd.ast;

import java.util.List;

public record MmdDiagramAst(boolean classDiagramDeclared, List<MmdTopLevelNode> nodes) {}
