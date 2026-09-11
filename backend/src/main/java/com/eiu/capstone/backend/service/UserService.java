package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkCreateResult;
import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.DTO.UserDTO.CreateUserRequest;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabDeadlineEmailSentRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.PasswordResetTokenRepository;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LabSubmissionRepository labSubmissionRepository;
    private final SubmissionPlagiarismMatchRepository plagiarismMatchRepository;
    private final SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository;
    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;
    private final SubmissionRelationResultRepository submissionRelationResultRepository;
    private final SubmissionTestcaseResultRepository submissionTestcaseResultRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final LabDeadlineEmailSentRepository labDeadlineEmailSentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserAccountRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            LabSubmissionRepository labSubmissionRepository,
            SubmissionPlagiarismMatchRepository plagiarismMatchRepository,
            SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository,
            SubmissionFieldResultRepository submissionFieldResultRepository,
            SubmissionMethodResultRepository submissionMethodResultRepository,
            SubmissionConstructorResultRepository submissionConstructorResultRepository,
            SubmissionChallengeResultRepository submissionChallengeResultRepository,
            SubmissionRelationResultRepository submissionRelationResultRepository,
            SubmissionTestcaseResultRepository submissionTestcaseResultRepository,
            StudentLabProgressRepository studentLabProgressRepository,
            TermEnrollmentRepository termEnrollmentRepository,
            LabDeadlineEmailSentRepository labDeadlineEmailSentRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.labSubmissionRepository = labSubmissionRepository;
        this.plagiarismMatchRepository = plagiarismMatchRepository;
        this.plagiarismFingerprintRepository = plagiarismFingerprintRepository;
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
        this.submissionRelationResultRepository = submissionRelationResultRepository;
        this.submissionTestcaseResultRepository = submissionTestcaseResultRepository;
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.labDeadlineEmailSentRepository = labDeadlineEmailSentRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public List<UserAccount> getAllUser() {
        return userRepository.findAllWithRoles();
    }

    @Transactional
    public Page<UserAccount> getAllUser(Pageable pageable) {
        return userRepository.findAllWithRoles(pageable);
    }

    @Transactional
    public UserAccount getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Transactional
    public UserAccount createUser(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("User request is required");
        }
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

        Set<String> roleNames = normalizeIncomingRoleNames(request.roleNames(), null);
        validateRoleNames(roleNames);
        validateCodesForRoles(roleNames, request.studentCode(), request.teacherCode());
        applyCodesForRoles(user, roleNames, request.studentCode(), request.teacherCode());
        user.setRoles(resolveRoles(roleNames));

        return userRepository.save(user);
    }

    @Transactional
    public List<BulkCreateResult> createUser(List<CreateUserRequest> requests) {
        List<BulkCreateResult> results = new ArrayList<>();
        if (requests == null) {
            return results;
        }

        for (CreateUserRequest request : requests) {
            if (request == null) {
                results.add(BulkCreateResult.failure(null, "Empty user request"));
                continue;
            }
            try {
                results.add(BulkCreateResult.success(createUser(request)));
            } catch (Exception ex) {
                results.add(BulkCreateResult.failure(request.email(), ex.getMessage()));
            }
        }
        return results;
    }

    // UserService.java - Change password   
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }
        if (newPassword.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be less than 100 characters");
        }

        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

        if (existingByEmail.isPresent() && existingByIrn.isPresent()
                && !existingByEmail.get().getId().equals(existingByIrn.get().getId())) {
            throw new IllegalArgumentException("IRN is already linked to another account");
        }

        UserAccount user = existingByEmail.orElseGet(() -> existingByIrn.orElseGet(UserAccount::new));
        boolean isNewUser = user.getId() == null;
        if (!isNewUser) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account already registered. Sign in with Google or IRN/password.");
        }

        user.setEmail(email);
        user.setFullName(fullName);
        user.setDateOfBirth(dateOfBirth);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStudentCode(irn);
        user.setTeacherCode(null);
        user.setRoles(resolveRoles(Set.of("STUDENT")));
        user.setIsActive(true);
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
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }

        return user;
    }

    @Transactional
    public UserDTO.UserResponse deleteUser(UUID id) {
        UserAccount user = userRepository.findAllWithRolesByIdIn(List.of(id)).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserDTO.UserResponse response = UserDTO.UserResponse.fromEntity(user);
        purgeUserData(id);
        userRepository.delete(user);
        return response;
    }

    private void purgeUserData(UUID userId) {
        studentLabProgressRepository.deleteByUser_Id(userId);
        termEnrollmentRepository.deleteByUser_Id(userId);
        labDeadlineEmailSentRepository.deleteByUser_Id(userId);
        passwordResetTokenRepository.deleteByUser_Id(userId);

        plagiarismMatchRepository.deleteInvolvingUser(userId);
        plagiarismFingerprintRepository.deleteAllByUserId(userId);
        submissionTestcaseResultRepository.deleteAssertionResultsByUserId(userId);
        submissionTestcaseResultRepository.deleteByUserId(userId);
        submissionFieldResultRepository.deleteByUserId(userId);
        submissionMethodResultRepository.deleteByUserId(userId);
        submissionConstructorResultRepository.deleteByUserId(userId);
        submissionChallengeResultRepository.deleteByUserId(userId);
        submissionRelationResultRepository.deleteByUserId(userId);
        labSubmissionRepository.deleteByUser_Id(userId);
    }

    @Transactional
    public UserDTO.UserResponse suspendStudent(UUID id) {
        return setStudentActive(id, false);
    }

    @Transactional
    public UserDTO.UserResponse restoreStudent(UUID id) {
        return setStudentActive(id, true);
    }

    private UserDTO.UserResponse setStudentActive(UUID id, boolean active) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!hasRole(user, "STUDENT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only students can be suspended");
        }
        if (hasRole(user, "LECTURER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot suspend a lecturer account");
        }
        user.setIsActive(active);
        return UserDTO.UserResponse.fromEntity(userRepository.save(user));
    }

    private boolean hasRole(UserAccount user, String roleName) {
        if (user.getRoles() == null) {
            return false;
        }
        String required = normalizeRoleName(roleName);
        return user.getRoles().stream()
                .map(role -> normalizeRoleName(role.getName()))
                .anyMatch(required::equals);
    }

    @Transactional
    public UserDTO.UserResponse updateUser(UUID userId, UserDTO.UpdateUserRequest request) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));

        userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Email already in use: " + request.getEmail());
                });

        Set<String> roleNames = normalizeIncomingRoleNames(request.getRoleNames(), request.getRole());
        String legacyIrn = blankToNull(request.getIrn());
        String studentCode = blankToNull(request.getStudentCode());
        String teacherCode = blankToNull(request.getTeacherCode());

        if (studentCode == null && legacyIrn != null && roleNames.contains("STUDENT") && !roleNames.contains("LECTURER")) {
            studentCode = legacyIrn;
        }
        if (teacherCode == null && legacyIrn != null && roleNames.contains("LECTURER") && !roleNames.contains("STUDENT")) {
            teacherCode = legacyIrn;
        }
        if (studentCode == null && legacyIrn != null && roleNames.contains("STUDENT") && roleNames.contains("LECTURER")
                && teacherCode == null) {
            studentCode = legacyIrn;
        }

        validateRoleNames(roleNames);
        validateCodesForRoles(roleNames, studentCode, teacherCode);

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        applyCodesForRoles(user, roleNames, studentCode, teacherCode);
        applyOptionalPassword(user, request.getPassword());
        user.setRoles(resolveRoles(roleNames));

        UserAccount saved = userRepository.save(user);
        return UserDTO.UserResponse.fromEntity(saved);
    }

    private Set<String> normalizeIncomingRoleNames(Set<String> roleNames, String legacyRole) {
        if (roleNames != null && !roleNames.isEmpty()) {
            return roleNames.stream()
                    .map(this::normalizeRoleName)
                    .filter(name -> "STUDENT".equals(name) || "LECTURER".equals(name))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (legacyRole != null && !legacyRole.isBlank()) {
            return Set.of(normalizeRoleName(legacyRole));
        }
        return Set.of("STUDENT");
    }

    private void validateRoleNames(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role is required");
        }
        for (String roleName : roleNames) {
            if (!"STUDENT".equals(roleName) && !"LECTURER".equals(roleName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role: " + roleName);
            }
        }
    }

    private void validateCodesForRoles(Set<String> roleNames, String studentCode, String teacherCode) {
        if (roleNames.contains("STUDENT") && (studentCode == null || studentCode.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student IRN is required for STUDENT role");
        }
        if (roleNames.contains("LECTURER") && (teacherCode == null || teacherCode.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecturer IRN is required for LECTURER role");
        }
    }

    private void applyCodesForRoles(UserAccount user, Set<String> roleNames, String studentCode, String teacherCode) {
        if (roleNames.contains("STUDENT")) {
            user.setStudentCode(studentCode);
        } else {
            user.setStudentCode(null);
        }
        if (roleNames.contains("LECTURER")) {
            user.setTeacherCode(teacherCode);
        } else {
            user.setTeacherCode(null);
        }
    }

    private void applyOptionalPassword(UserAccount user, String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        if (password.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be less than 100 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
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
        String normalized = roleName.trim().toUpperCase();
        if ("TEACHER".equals(normalized) || "LECTURER".equals(normalized)) {
            return "LECTURER";
        }
        return normalized;
    }

    private boolean isLecturerRole(String roleName) {
        return "LECTURER".equalsIgnoreCase(normalizeRoleName(roleName));
    }

    private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
    
}
}