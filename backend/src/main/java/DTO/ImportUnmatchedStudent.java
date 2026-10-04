package com.eiu.capstone.backend.DTO;

public record ImportUnmatchedStudent(
        String fullName,
        String studentCode,
        String email,
        String reason) {
}
