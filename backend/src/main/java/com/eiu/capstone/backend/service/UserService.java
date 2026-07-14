package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount createUser(CreateUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setStudentCode(request.studentCode());
        user.setTeacherCode(request.teacherCode());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            user.setRoles(resolveRoles(request.roleNames()));
        } else {
            user.setRoles(resolveRoles(Set.of("STUDENT")));
        }

        return userRepository.save(user);
    }

    @Transactional
    public UserAccount createOrUpdateGoogleUser(String email, String fullName, LocalDate dateOfBirth, String irn, String password, String roleName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (irn == null || irn.isBlank()) {
            throw new IllegalArgumentException("IRN is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        Optional<UserAccount> existingByEmail = userRepository.findByEmail(email);
        Optional<UserAccount> existingByIrn = userRepository.findByStudentCodeOrTeacherCode(irn, irn);

        UserAccount user = existingByEmail.orElseGet(() -> existingByIrn.orElseGet(UserAccount::new));
        boolean isNewUser = user.getId() == null;

        user.setEmail(email);
        user.setFullName(fullName);
        user.setDateOfBirth(dateOfBirth);
        user.setPasswordHash(passwordEncoder.encode(password));

        if (isLecturerRole(roleName)) {
            user.setTeacherCode(irn);
            user.setStudentCode(null);
        } else {
            user.setStudentCode(irn);
            user.setTeacherCode(null);
        }

        user.setRoles(resolveRoles(Set.of(normalizeRoleName(roleName))));

        if (isNewUser) {
            return userRepository.save(user);
        }
        return userRepository.save(user);
    }

    @Transactional
    public UserAccount authenticateByIrn(String irn, String password) {
        if (irn == null || irn.isBlank()) {
            throw new IllegalArgumentException("IRN is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        UserAccount user = userRepository.findByStudentCodeOrTeacherCode(irn, irn)
                .orElseThrow(() -> new BadCredentialsException("Invalid IRN or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid IRN or password");
        }

        return user;
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return resolveRoles(Set.of("STUDENT"));
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            String normalized = normalizeRoleName(roleName);
            Role role = roleRepository.findByName(normalized)
                    .orElseGet(() -> roleRepository.save(new Role(normalized)));
            roles.add(role);
        }
        return roles;
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "STUDENT";
        }
        return roleName.trim().toUpperCase();
    }

    private boolean isLecturerRole(String roleName) {
        return "LECTURER".equalsIgnoreCase(normalizeRoleName(roleName));
    }
}