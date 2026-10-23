package com.eiu.capstone.backend.service;

/**
 * Controls how Class/MMD result tabs label rubric-scoped rows.
 * {@link #STUDENT} redacts rubric expected values when the submission snapshot has no entry.
 * {@link #LECTURER} preserves full rubric checklist labels for review.
 */
public enum DisclosureMode {
    STUDENT,
    LECTURER
}
