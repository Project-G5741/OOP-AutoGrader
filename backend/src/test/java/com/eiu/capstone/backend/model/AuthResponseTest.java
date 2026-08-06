package com.eiu.capstone.backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthResponseTest {

    @Test
    void exposesReadableIdentifierFields() {
        AuthResponse response = new AuthResponse(
                "token",
                UUID.randomUUID(),
                "student@example.com",
                "Student Name",
                "Student Name",
                List.of("STUDENT"),
                "2331200082",
                "2331200082",
                null
        );

        assertEquals("2331200082", response.irn());
        assertEquals("2331200082", response.studentCode());
    }
}
