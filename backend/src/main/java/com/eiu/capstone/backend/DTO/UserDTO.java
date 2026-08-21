package com.eiu.capstone.backend.DTO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.eiu.capstone.backend.model.UserAccount;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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

        /** @deprecated use studentCode / teacherCode */
        private String irn;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        /** @deprecated use roleNames */
        private String role;

        private Set<String> roleNames;

        private String studentCode;

        private String teacherCode;

        @JsonAlias({"newPassword"})
        private String password;

        public UpdateUserRequest() {}

        public String getIrn() { return irn; }
        public void setIrn(String irn) { this.irn = irn; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Set<String> getRoleNames() { return roleNames; }
        public void setRoleNames(Set<String> roleNames) { this.roleNames = roleNames; }

        @JsonSetter("roleNames")
        public void setRoleNamesFromJson(Collection<String> values) {
            if (values == null) {
                this.roleNames = null;
                return;
            }
            this.roleNames = new LinkedHashSet<>(values);
        }

        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public String getTeacherCode() { return teacherCode; }
        public void setTeacherCode(String teacherCode) { this.teacherCode = teacherCode; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UserResponse {

        private UUID id;
        private String irn;
        private String fullName;
        private String email;
        @JsonProperty("isActive")
        private boolean isActive;
        private Set<String> roles;
        private String studentCode;
        private String teacherCode;

        public static UserResponse fromEntity(UserAccount user) {
            UserResponse response = new UserResponse();
            response.id = user.getId();
            response.fullName = user.getFullName();
            response.email = user.getEmail();
            response.isActive = user.getIsActive();
            response.studentCode = user.getStudentCode();
            response.teacherCode = user.getTeacherCode();

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
        public String getStudentCode() { return studentCode; }
        public String getTeacherCode() { return teacherCode; }
    }
}
