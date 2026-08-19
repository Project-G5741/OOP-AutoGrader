package com.eiu.capstone.backend.grading.mmd.ast;

public record MmdColonMemberNode(String className, String memberLine, int line) implements MmdTopLevelNode {}
