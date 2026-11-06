package com.eiu.capstone.backend.DTO;

public record ImportStudentRow(String studentCode, String email, String fullName) {
    public ImportStudentRow(String studentCode, String email) {
        this(studentCode, email, null);
    }
}
