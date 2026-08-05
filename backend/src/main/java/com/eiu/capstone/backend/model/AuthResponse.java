package com.eiu.capstone.backend.model;

import java.util.List;
import java.util.UUID;

public record AuthResponse(String accessToken, UUID id, String email, String name, String domain, List<String> roles,
                           String irn, String studentCode, String lecturerCode) {
}
