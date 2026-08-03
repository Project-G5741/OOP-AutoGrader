package com.eiu.capstone.backend.DTO;

import java.util.List;

/** One class box in the MMD tab. */
public record MmdClassDTO(String name, List<MmdAttributeDTO> attributes) {
}