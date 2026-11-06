package com.eiu.capstone.backend.DTO;

import java.util.List;

/**
 * `type` is e.g. "ABSTRACT CLASS", "CLASS", "INTERFACE" — from the student's parsed
 * submission when a snapshot exists; otherwise from rubric class_entity fields.
 * `status` is one of "success" | "warning" | "error" | "info", computed from
 * class-shell checks (binary) plus fields/constructors/methods correctness.
 * A matching shell with no members is "success" (not "info").
 */
public record ClassDetailDTO(
        String name,
        String type,
        String status,
        String error,
        List<ClassFieldDetailDTO> fields,
        List<ClassConstructorDetailDTO> constructors,
        List<ClassMethodDetailDTO> methods
) {
}
