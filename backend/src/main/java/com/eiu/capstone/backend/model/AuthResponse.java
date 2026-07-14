package com.eiu.capstone.backend.model;

import java.util.List;

public record AuthResponse(String accessToken, String email, String name, String domain, List<String> roles) {
}
