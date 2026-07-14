package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;
import java.util.Set;

public class UserDTO {
    public record CreateUserRequest(
            String fullName,
            String email,
            String password,
            String studentCode,
            String teacherCode,
            LocalDate dateOfBirth,
            Set<String> roleNames) {
    }
}
