package com.eiu.capstone.backend.DTO;

/**
 * All three fields are null when the student has no student_lab_progress row
 * for the lab yet — the frontend renders "--/--" for each in that case.
 */
public record StatsDTO(Integer currentGrade, Integer totalSubmissions, String latestSubmission) {
}