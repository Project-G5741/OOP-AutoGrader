package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.eiu.capstone.backend.model.UserAccount;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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

    public static class UpdateUserRequest {
 
        @NotBlank(message = "IRN is required")
        private String irn;
 
        @NotBlank(message = "Full name is required")
        private String fullName;
 
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;
 
        @NotBlank(message = "Role is required")
        private String role; // e.g. "STUDENT", "TEACHER", "ADMIN"
 
        public UpdateUserRequest() {}
 
        public String getIrn() { return irn; }
        public void setIrn(String irn) { this.irn = irn; }
 
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
 
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
 
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
 
    /**
     * Response payload returned after updating a user.
     * Built inside the transaction (in the service) so the lazy `roles`
     * collection is safely initialized before Jackson ever sees it,
     * and keeps passwordHash out of the response entirely.
     */
    public static class UserResponse {
 
        private UUID id;
        private String irn;
        private String fullName;
        private String email;
        private boolean isActive;
        private Set<String> roles;
 
        public static UserResponse fromEntity(UserAccount user) {
            UserResponse response = new UserResponse();
            response.id = user.getId();
            response.fullName = user.getFullName();
            response.email = user.getEmail();
            response.isActive = user.getIsActive();
 
            // Triggers the lazy load here, while still inside the @Transactional service method
            response.roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());
 
            response.irn = user.getStudentCode() != null
                    ? user.getStudentCode()
                    : user.getTeacherCode();
 
            return response;
        }
 
        public UUID getId() { return id; }
        public String getIrn() { return irn; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public boolean isActive() { return isActive; }
        public Set<String> getRoles() { return roles; }
    }
}
