package com.eiu.capstone.backend.DTO;

public record ClassMethodDetailDTO(String name, String scope, String returnType, boolean ok, boolean partial) {
    public ClassMethodDetailDTO(String name, String scope, String returnType, boolean ok) {
        this(name, scope, returnType, ok, false);
    }
}
