package com.eiu.capstone.backend.DTO;

import java.util.UUID;

public record CloneSourceTermDTO(
        UUID id,
        String label,
        String yearLabel,
        int termNumber) {
}
