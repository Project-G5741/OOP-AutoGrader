package com.eiu.capstone.backend.model;

public record AuthResponse(String accessToken, String email, String name, String domain) {
}
