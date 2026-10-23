package com.eiu.capstone.backend.model;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String irn, @NotBlank String password) {
}
