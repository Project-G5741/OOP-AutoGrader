package com.eiu.capstone.backend.DTO;

import java.util.UUID;

public record TermStudentDTO(UUID id, String fullName, String studentCode, String email, boolean isActive) {
}
