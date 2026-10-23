package com.eiu.capstone.backend.grading.mmd.ast;

/** Cosmetic or non-graded directives (note, direction, style). */
public record MmdIgnoredDirectiveNode(String rawLine, int line) implements MmdTopLevelNode {}
