package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkCreateResult;
import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.DTO.UserDTO.UpdateUserRequest;
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
    public List<UserAccount> getAllUser() {
        return userRepository.findAll();
    }

    @Transactional
    public UserAccount getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
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
        user.setStudentCode(blankToNull(request.studentCode()));
        user.setTeacherCode(blankToNull(request.teacherCode()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsActive(true);

        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            user.setRoles(resolveRoles(request.roleNames()));
        } else {
            user.setRoles(resolveRoles(Set.of("STUDENT")));
        }

        return userRepository.save(user);
    }

    public List<BulkCreateResult> createUsers(List<CreateUserRequest> requests) {
        List<BulkCreateResult> results = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateUserRequest request = requests.get(i);
            try {
                UserAccount created = createUser(request); // reuses your existing single-user method
                results.add(BulkCreateResult.success(created));
            } catch (Exception e) {
                results.add(BulkCreateResult.failure(request.email(), e.getMessage()));
            }

            if (i < requests.size() - 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return results;
    }

    @Transactional
    public UserAccount createOrUpdateGoogleUser(String email, String fullName, LocalDate dateOfBirth, String irn,
            String password, String roleName) {
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
    public UserAccount deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found: " + id);
        }
        UserAccount user = userRepository.getReferenceById(id);
        user.setIsActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public UserDTO.UserResponse updateUser(UUID userId, UserDTO.UpdateUserRequest request) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));
 
        // Enforce email uniqueness, excluding the current user.
        // Uses findByEmail instead of existsByEmailAndIdNot so no new
        // repository method is required; swap this back if you'd rather
        // add existsByEmailAndIdNot(String, UUID) to the repository.
        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Email already in use: " + request.getEmail());
                });
 
        // Resolve the role first, since it decides where the IRN goes
        String roleName = request.getRole().trim().toUpperCase();
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Role not found: " + roleName));
 
        // Route the IRN to the correct field based on role, clearing the other
        if ("STUDENT".equals(roleName)) {
            user.setStudentCode(request.getIrn());
            user.setTeacherCode(null);
        } else if ("TEACHER".equals(roleName)) {
            user.setTeacherCode(request.getIrn());
            user.setStudentCode(null);
        } else {
            // e.g. ADMIN or other roles with no code convention yet;
            // defaults to studentCode, adjust if your schema differs
            user.setStudentCode(request.getIrn());
            user.setTeacherCode(null);
        }
 
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
 
        HashSet<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
 
        UserAccount saved = userRepository.save(user);
 
        // Built here, still inside @Transactional, so the lazy `roles`
        // collection is safe to read
        return UserDTO.UserResponse.fromEntity(saved);
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

    private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
}
}