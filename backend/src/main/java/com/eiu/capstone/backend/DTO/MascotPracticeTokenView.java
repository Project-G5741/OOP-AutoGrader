package com.eiu.capstone.backend.DTO;

import java.time.Instant;

public record MascotPracticeTokenView(String token, Instant expiresAt) {
}
