package com.eiu.capstone.backend.model;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(@NotBlank String token) {
}
