package com.eiu.capstone.backend.model;

import java.time.LocalDate;

public record GoogleLoginUpsertRequest(
        String token,
        String irn,
        String password,
        String role,
        LocalDate dateOfBirth
) {
}
