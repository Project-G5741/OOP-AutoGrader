package com.eiu.capstone.backend.desktop;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

/**
 * Replace-imports current-term enrolled IRNs for lecturer offline Bulk Grading gates.
 */
@Service
@Profile("desktop-lecturer")
public class DesktopRosterImportService {

    private static final Pattern TEN_DIGIT = Pattern.compile("^\\d{10}$");
    private static final String OFFLINE_YEAR_LABEL = "Offline";
    private static final int OFFLINE_TERM_NUMBER = 1;

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DesktopRosterImportService(
            TermRepository termRepository,
            AcademicYearRepository academicYearRepository,
            TermEnrollmentRepository termEnrollmentRepository,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.termRepository = termRepository;
        this.academicYearRepository = academicYearRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record ImportResult(int importedCount, boolean blocked) {}

    /**
     * Successful import of zero IRNs clears membership and blocks Bulk.
     * Caller must reject unparseable files before invoking this method.
     */
    @Transactional
    public ImportResult replaceImport(List<String> rawIrns) {
        Set<String> irns = normalizeIrns(rawIrns);
        Term term = ensureCurrentOfflineTerm();
        clearEnrollments(term);

        if (irns.isEmpty()) {
            return new ImportResult(0, true);
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("STUDENT")));
        for (String irn : irns) {
            UserAccount user = upsertStubStudent(irn, studentRole);
            TermEnrollment enrollment = new TermEnrollment();
            enrollment.setUser(user);
            enrollment.setTerm(term);
            termEnrollmentRepository.save(enrollment);
        }
        return new ImportResult(irns.size(), false);
    }

    public boolean hasUsableRoster() {
        if (termRepository.findCurrent().isEmpty()) {
            return false;
        }
        List<String> codes = termEnrollmentRepository.findStudentCodesEnrolledInCurrentTerm();
        return codes != null && !codes.isEmpty();
    }

    private Set<String> normalizeIrns(List<String> rawIrns) {
        Set<String> out = new HashSet<>();
        if (rawIrns == null) {
            return out;
        }
        for (String raw : rawIrns) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!TEN_DIGIT.matcher(trimmed).matches()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each student_code must be exactly 10 digits: " + trimmed);
            }
            out.add(trimmed);
        }
        return out;
    }

    private Term ensureCurrentOfflineTerm() {
        return termRepository.findCurrent().orElseGet(this::createCurrentOfflineTerm);
    }

    private Term createCurrentOfflineTerm() {
        AcademicYear year = academicYearRepository.findByYearLabel(OFFLINE_YEAR_LABEL)
                .orElseGet(() -> {
                    AcademicYear created = new AcademicYear();
                    created.setYearLabel(OFFLINE_YEAR_LABEL);
                    return academicYearRepository.save(created);
                });
        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(OFFLINE_TERM_NUMBER);
        term.setCurrent(true);
        Term saved = termRepository.save(term);
        termRepository.clearOtherCurrent(saved.getId());
        return saved;
    }

    private void clearEnrollments(Term term) {
        termEnrollmentRepository.deleteByTerm_Id(term.getId());
    }

    private UserAccount upsertStubStudent(String irn, Role studentRole) {
        return userAccountRepository.findByStudentCode(irn).orElseGet(() -> {
            UserAccount user = new UserAccount();
            user.setEmail(("offline+" + irn + "@desktop.local").toLowerCase(Locale.ROOT));
            user.setFullName("Student " + irn);
            user.setStudentCode(irn);
            user.setPasswordHash(passwordEncoder.encode("offline-roster-not-used"));
            user.setIsActive(true);
            user.setRoles(Set.of(studentRole));
            return userAccountRepository.save(user);
        });
    }
}
