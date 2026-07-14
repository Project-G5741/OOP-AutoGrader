package com.eiu.capstone.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

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
    System.out.println("DEBUG full request = " + request);
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
        List<Role> roles = roleRepository.findByNameIn(request.roleNames());
        user.setRoles(new HashSet<>(roles));
    }

    return userRepository.save(user);
}

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found: " + id);
        }
        userRepository.deleteById(id);
        // user_role rows are cleaned up automatically by the DB's
        // "ON DELETE CASCADE" on user_role_user_id_fkey — no manual cleanup needed.
    }
}