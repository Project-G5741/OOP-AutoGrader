package com.eiu.capstone.backend.model;

public record GoogleLoginUpsertRequest(
        String token,
        String irn,
        String password,
        String role
) {
}
