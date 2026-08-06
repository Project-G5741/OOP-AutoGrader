package com.eiu.capstone.backend.DTO;

/**
 * One row in an MMD class box, e.g. "speed: double" (type="field"),
 * "Vehicle(brand)" (type="constructor"), "move(): void" (type="method").
 * `type` drives the color coding on the frontend (Tick + text color).
 */
public record MmdAttributeDTO(String name, String type, boolean ok, String error) {
    public MmdAttributeDTO(String name, String type, boolean ok) {
        this(name, type, ok, null);
    }
}