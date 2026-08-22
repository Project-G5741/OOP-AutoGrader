package com.eiu.capstone.backend.DTO;

public record ClassConstructorDetailDTO(String name, String scope, String params, boolean ok, boolean partial) {
    public ClassConstructorDetailDTO(String name, String scope, String params, boolean ok) {
        this(name, scope, params, ok, false);
    }
}
