package com.eiu.capstone.backend.DTO;

import java.util.List;

/** One class box in the MMD tab. */
public record MmdClassDTO(String name, List<MmdAttributeDTO> attributes, List<MmdRelationDTO> relations) {
    public MmdClassDTO(String name, List<MmdAttributeDTO> attributes) {
        this(name, attributes, List.of());
    }
}