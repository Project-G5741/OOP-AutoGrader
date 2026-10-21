package com.eiu.capstone.backend.service;

/**
 * Purchase refused before any point change is committed.
 * {@code code} is a stable token the API returns; the SPA maps it to its own copy.
 */
public class MascotPurchaseException extends RuntimeException {

    public static final String ALREADY_OWNED = "ALREADY_OWNED";
    public static final String INSUFFICIENT_POINTS = "INSUFFICIENT_POINTS";
    public static final String NOT_OWNED = "NOT_OWNED";

    private final String code;

    public MascotPurchaseException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
