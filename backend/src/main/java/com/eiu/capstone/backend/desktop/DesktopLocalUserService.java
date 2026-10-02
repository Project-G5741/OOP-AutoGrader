package com.eiu.capstone.backend.desktop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.StudentTermAccessService;

@Service
@Profile("desktop")
public class DesktopLocalUserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final LabRepository labRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DesktopLocalUserService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            LabRepository labRepository,
            TermEnrollmentRepository termEnrollmentRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.labRepository = labRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount ensureLocalStudent(Term term) {
        UserAccount user = userAccountRepository.findByEmail(DesktopLocalIdentity.EMAIL)
                .orElseGet(this::createLocalStudent);
        if (term != null) {
            ensureEnrolled(user, term);
        }
        return user;
    }

    public UserAccount localStudent() {
        return userAccountRepository.findByEmail(DesktopLocalIdentity.EMAIL)
                .orElseThrow(() -> new IllegalStateException("Desktop local student is not initialized"));
    }

    public StudentTermAccessService.UploadAccess requireUploadAccess(UUID labId) {
        UserAccount user = localStudent();
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Lab not found"));
        return new StudentTermAccessService.UploadAccess(user, lab);
    }

    private UserAccount createLocalStudent() {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("STUDENT")));
        UserAccount user = new UserAccount();
        user.setEmail(DesktopLocalIdentity.EMAIL);
        user.setFullName(DesktopLocalIdentity.FULL_NAME);
        user.setStudentCode(DesktopLocalIdentity.IRN);
        user.setPasswordHash(passwordEncoder.encode("desktop-practice-not-used"));
        user.setIsActive(true);
        user.setRoles(Set.of(studentRole));
        return userAccountRepository.save(user);
    }

    private void ensureEnrolled(UserAccount user, Term term) {
        if (termEnrollmentRepository.existsByUser_IdAndTerm_Id(user.getId(), term.getId())) {
            return;
        }
        TermEnrollment enrollment = new TermEnrollment();
        enrollment.setUser(user);
        enrollment.setTerm(term);
        termEnrollmentRepository.save(enrollment);
    }

    public Optional<String> readLoadedPackVersion(Path versionFile) {
        try {
            if (versionFile == null || !Files.isRegularFile(versionFile)) {
                return Optional.empty();
            }
            String line = Files.readString(versionFile, StandardCharsets.UTF_8).trim();
            return line.isEmpty() ? Optional.empty() : Optional.of(line);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void writeLoadedPackVersion(Path versionFile, String packVersion) throws Exception {
        if (versionFile == null || packVersion == null || packVersion.isBlank()) {
            return;
        }
        Files.createDirectories(versionFile.getParent());
        Files.writeString(versionFile, packVersion, StandardCharsets.UTF_8);
    }
}
