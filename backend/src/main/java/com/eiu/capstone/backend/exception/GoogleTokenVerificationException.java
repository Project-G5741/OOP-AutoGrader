package com.eiu.capstone.backend.exception;

public class GoogleTokenVerificationException extends RuntimeException {

    private final String detail;

    public GoogleTokenVerificationException(String message, String detail) {
        super(message);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
