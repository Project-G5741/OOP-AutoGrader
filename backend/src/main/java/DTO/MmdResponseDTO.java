package com.eiu.capstone.backend.DTO;

import java.util.List;

public record MmdResponseDTO(List<MmdClassDTO> classes, String parseError) {

    public MmdResponseDTO {
        classes = classes != null ? List.copyOf(classes) : List.of();
    }
}
