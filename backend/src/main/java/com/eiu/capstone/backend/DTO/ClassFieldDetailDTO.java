package com.eiu.capstone.backend.DTO;

public record ClassFieldDetailDTO(String name, String scope, String dataType, boolean ok, boolean partial) {
    public ClassFieldDetailDTO(String name, String scope, String dataType, boolean ok) {
        this(name, scope, dataType, ok, false);
    }
}
