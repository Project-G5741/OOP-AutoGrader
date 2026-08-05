package com.eiu.capstone.backend.DTO;

/** `params` is a pre-formatted "Type name, Type name" string, e.g. "String brand, double speed". */
public record ClassConstructorDetailDTO(String name, String scope, String params, boolean ok) {
}