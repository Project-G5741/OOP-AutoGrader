package com.eiu.capstone.backend.DTO;

public record MmdRelationDTO(String from, String to, String relType, boolean ok, String error) {
    public MmdRelationDTO(String from, String to, String relType, boolean ok) {
        this(from, to, relType, ok, null);
    }
}
