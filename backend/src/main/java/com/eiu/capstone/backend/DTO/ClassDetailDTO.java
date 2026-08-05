package com.eiu.capstone.backend.DTO;

import java.util.List;

/**
 * `type` is e.g. "ABSTRACT CLASS", "CLASS", "INTERFACE" — derived from
 * class_entity.is_abstract + class_entity.declaring_type (resolved via master_data).
 * `status` is one of "success" | "warning" | "error" | "info", computed from
 * how many of this class's fields/constructors/methods were graded correct.
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