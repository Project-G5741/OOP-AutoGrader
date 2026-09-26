package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CloneLabErrorDTO;
import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.CreateTermRequest;
import com.eiu.capstone.backend.DTO.ImportStudentRow;
import com.eiu.capstone.backend.DTO.ImportStudentsRequest;
import com.eiu.capstone.backend.DTO.ImportStudentsResult;
import com.eiu.capstone.backend.DTO.ImportUnmatchedStudent;
import com.eiu.capstone.backend.DTO.TermRosterDTO;
import com.eiu.capstone.backend.DTO.TermStudentDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
public class TermService {

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final LabRepository labRepository;
    private final LecturerOverviewCache lecturerOverviewCache;
    private final SessionValidityService sessionValidityService;
    private final LabCloneService labCloneService;
    private final LabStructureService labStructureService;
    private final TransactionTemplate transactionTemplate;

    public TermService(TermRepository termRepository,
                       AcademicYearRepository academicYearRepository,
                       TermEnrollmentRepository termEnrollmentRepository,
                       UserAccountRepository userAccountRepository,
                       LabRepository labRepository,
                       LecturerOverviewCache lecturerOverviewCache,
                       SessionValidityService sessionValidityService,
                       LabCloneService labCloneService,
                       LabStructureService labStructureService,
                       PlatformTransactionManager transactionManager) {
        this.termRepository = termRepository;
        this.academicYearRepository = academicYearRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.labRepository = labRepository;
        this.lecturerOverviewCache = lecturerOverviewCache;
        this.sessionValidityService = sessionValidityService;
        this.labCloneService = labCloneService;
        this.labStructureService = labStructureService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<TermSummaryDTO> listTerms() {
        Map<UUID, Integer> counts = enrollmentCountsByTerm();
        return termRepository.findAllWithAcademicYear().stream()
                .sorted(Comparator
                        .comparing((Term t) -> t.getAcademicYear().getYearLabel()).reversed()
                        .thenComparing(Term::getTermNumber, Comparator.reverseOrder()))
                .map(term -> toSummary(term, counts.getOrDefault(term.getId(), 0)))
                .toList();
    }

    public TermSummaryDTO createTerm(CreateTermRequest request) {
        Optional<Term> outgoingCurrent = findCurrentTerm();
        List<UUID> copyLabIds = request.copyLabIds() != null ? request.copyLabIds() : List.of();
        if (!copyLabIds.isEmpty()) {
            if (outgoingCurrent.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No current quarter to copy labs from");
            }
            labCloneService.requireLabsInTerm(copyLabIds, outgoingCurrent.get().getId());
        }

        TermSummaryDTO summary = transactionTemplate.execute(status -> persistNewTerm(request));
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create quarter");
        }
        if (copyLabIds.isEmpty()) {
            return summary;
        }
        CloneLabsResponse cloneResult = labCloneService.cloneLabs(
                copyLabIds, summary.id(), outgoingCurrent.get().getId());
        List<String> cloneErrors = cloneResult.errors().stream()
                .map(CloneLabErrorDTO::message)
                .toList();
        return new TermSummaryDTO(
                summary.id(),
                summary.label(),
                summary.endDate(),
                summary.yearLabel(),
                summary.termNumber(),
                summary.current(),
                summary.studentCount(),
                cloneErrors);
    }

    private TermSummaryDTO persistNewTerm(CreateTermRequest request) {
        String yearLabel = request.yearLabel().trim();
        int termNumber = request.termNumber();
        AcademicYear year = academicYearRepository.findByYearLabel(yearLabel)
                .orElseGet(() -> {
                    AcademicYear created = new AcademicYear();
                    created.setYearLabel(yearLabel);
                    created.setStartDate(request.startDate());
                    created.setEndDate(request.endDate());
                    return academicYearRepository.save(created);
                });
        if (termRepository.findByAcademicYear_IdAndTermNumber(year.getId(), termNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That quarter already exists for " + yearLabel);
        }
        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setCurrent(false);
        term = termRepository.save(term);
        if (request.setCurrent()) {
            termRepository.clearOtherCurrent(term.getId());
            term.setCurrent(true);
            term = termRepository.save(term);
            lecturerOverviewCache.invalidate();
        }
        return toSummary(term);
    }

    @Transactional
    public TermSummaryDTO setCurrentTerm(UUID termId) {
        Term target = termRepository.findById(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter not found"));
        termRepository.clearOtherCurrent(termId);
        target.setCurrent(true);
        TermSummaryDTO summary = toSummary(termRepository.save(target));
        lecturerOverviewCache.invalidate();
        return summary;
    }

    @Transactional(readOnly = true)
    public TermRosterDTO listRoster(UUID termId) {
        requireTermExists(termId);
        List<TermEnrollment> enrollments = termEnrollmentRepository.findByTermIdWithUser(termId);
        List<TermStudentDTO> enrolled = new ArrayList<>();
        Set<UUID> enrolledIds = new HashSet<>();
        for (TermEnrollment enrollment : enrollments) {
            UserAccount user = enrollment.getUser();
            if (user == null) {
                continue;
            }
            enrolledIds.add(user.getId());
            if (isStudent(user) && user.getIsActive()) {
                enrolled.add(toStudent(user));
            }
        }
        return new TermRosterDTO(enrolled, availableStudents(enrolledIds));
    }

    @Transactional(readOnly = true)
    public List<TermStudentDTO> listEnrolledStudents(UUID termId) {
        requireTermExists(termId);
        return enrolledSnapshot(termId);
    }

    @Transactional(readOnly = true)
    public List<TermStudentDTO> listAvailableStudents(UUID termId) {
        requireTermExists(termId);
        Set<UUID> enrolledIds = new HashSet<>(termEnrollmentRepository.findUserIdsByTermId(termId));
        return availableStudents(enrolledIds);
    }

    @Transactional
    public List<TermStudentDTO> enrollStudents(UUID termId, List<UUID> studentIds) {
        Term term = requireTerm(termId);
        List<UUID> ids = studentIds == null ? List.of() : studentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return enrolledSnapshot(termId);
        }
        Map<UUID, UserAccount> users = new HashMap<>();
        for (UserAccount user : userAccountRepository.findAllWithRolesByIdIn(ids)) {
            users.put(user.getId(), user);
        }
        Set<UUID> alreadyEnrolled = new HashSet<>(termEnrollmentRepository.findUserIdsByTermId(term.getId()));
        List<TermEnrollment> toSave = new ArrayList<>();
        for (UUID studentId : ids) {
            UserAccount user = users.get(studentId);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
            }
            if (!user.getIsActive() || !isStudent(user)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only active students can be added to a term");
            }
            if (!alreadyEnrolled.add(user.getId())) {
                continue;
            }
            TermEnrollment enrollment = new TermEnrollment();
            enrollment.setUser(user);
            enrollment.setTerm(term);
            toSave.add(enrollment);
        }
        if (!toSave.isEmpty()) {
            termEnrollmentRepository.saveAll(toSave);
        }
        return enrolledSnapshot(termId);
    }

    @Transactional
    public ImportStudentsResult importStudents(UUID termId, ImportStudentsRequest request) {
        Term term = requireTerm(termId);
        int enrolled = 0;
        int alreadyInTerm = 0;
        int notFound = 0;
        int skipped = 0;
        List<String> unmatched = new ArrayList<>();
        List<ImportUnmatchedStudent> notFoundStudents = new ArrayList<>();
        List<ImportUnmatchedStudent> alreadyInTermStudents = new ArrayList<>();
        Set<UUID> seenUsers = new HashSet<>();
        Set<String> codes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (ImportStudentRow row : request.rows()) {
            String studentCode = normalizeStudentCode(row == null ? null : row.studentCode());
            String email = normalizeEmail(row == null ? null : row.email());
            if (!studentCode.isEmpty()) {
                codes.add(studentCode.toLowerCase(Locale.ROOT));
            }
            if (!email.isEmpty()) {
                emails.add(email.toLowerCase(Locale.ROOT));
            }
        }
        Map<String, UserAccount> byCode = loadUsersByStudentCode(codes);
        Map<String, UserAccount> byEmail = loadUsersByEmail(emails);
        Set<UUID> alreadyEnrolled = new HashSet<>(termEnrollmentRepository.findUserIdsByTermId(term.getId()));
        List<TermEnrollment> toSave = new ArrayList<>();

        for (ImportStudentRow row : request.rows()) {
            String studentCode = normalizeStudentCode(row == null ? null : row.studentCode());
            String email = normalizeEmail(row == null ? null : row.email());
            String fullName = normalizeFullName(row == null ? null : row.fullName());
            if (studentCode.isEmpty() && email.isEmpty()) {
                skipped++;
                continue;
            }
            UserAccount user = resolveExistingStudent(studentCode, email, byCode, byEmail);
            if (user == null) {
                notFound++;
                recordNotice(
                        notFoundStudents,
                        unmatched,
                        fullName,
                        studentCode,
                        email,
                        "Not in the system — no matching student account was found.");
                continue;
            }
            if (!user.getIsActive() || !isStudent(user)) {
                notFound++;
                recordNotice(
                        notFoundStudents,
                        unmatched,
                        firstNonBlank(fullName, user.getFullName()),
                        firstNonBlank(studentCode, user.getStudentCode()),
                        firstNonBlank(email, user.getEmail()),
                        "Could not be added because the account is inactive or is not a student.");
                continue;
            }
            if (!seenUsers.add(user.getId()) || alreadyEnrolled.contains(user.getId())) {
                alreadyInTerm++;
                recordNotice(
                        alreadyInTermStudents,
                        null,
                        firstNonBlank(fullName, user.getFullName()),
                        firstNonBlank(studentCode, user.getStudentCode()),
                        firstNonBlank(email, user.getEmail()),
                        "Already enrolled in this quarter.");
                continue;
            }
            TermEnrollment enrollment = new TermEnrollment();
            enrollment.setUser(user);
            enrollment.setTerm(term);
            toSave.add(enrollment);
            alreadyEnrolled.add(user.getId());
            enrolled++;
        }
        if (!toSave.isEmpty()) {
            termEnrollmentRepository.saveAll(toSave);
        }

        return new ImportStudentsResult(
                enrolled,
                alreadyInTerm,
                notFound,
                skipped,
                unmatched,
                notFoundStudents,
                alreadyInTermStudents,
                enrolledSnapshot(termId));
    }

    @Transactional
    public void removeStudent(UUID termId, UUID studentId) {
        Term term = requireTerm(termId);
        TermEnrollment enrollment = termEnrollmentRepository.findByUser_IdAndTerm_Id(studentId, termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not in this term"));
        termEnrollmentRepository.delete(enrollment);
        if (term.isCurrent()) {
            sessionValidityService.bumpSessionVersion(studentId);
        }
    }

    @Transactional
    public void deleteTerm(UUID termId) {
        Term term = requireTerm(termId);
        if (term.isCurrent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete the current quarter. Set another quarter as current first.");
        }
        // Non-current quarters are hidden from Solution Management, so leftover clone shells
        // would otherwise block delete forever. Bulk SQL wipe (few Neon RTTs) then enrollments.
        List<UUID> labIds = labRepository.findByTerm_Id(termId).stream()
                .map(Lab::getId)
                .toList();
        labStructureService.deleteLabsCascadeBulk(labIds);
        termEnrollmentRepository.deleteByTerm_Id(termId);
        termRepository.delete(term);
        lecturerOverviewCache.invalidate();
    }

    @Transactional(readOnly = true)
    public Optional<Term> findCurrentTerm() {
        return termRepository.findCurrent();
    }

    @Transactional(readOnly = true)
    public boolean isInCurrentTerm(UUID userId) {
        if (userId == null) {
            return false;
        }
        return termEnrollmentRepository.existsByUser_IdAndTerm_CurrentTrue(userId);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(UUID userId, UUID termId) {
        if (userId == null || termId == null) {
            return false;
        }
        return termEnrollmentRepository.existsByUser_IdAndTerm_Id(userId, termId);
    }

    private List<TermStudentDTO> availableStudents(Set<UUID> enrolledIds) {
        List<TermStudentDTO> available = new ArrayList<>();
        for (UserAccount user : userAccountRepository.findActiveStudents()) {
            if (!enrolledIds.contains(user.getId())) {
                available.add(toStudent(user));
            }
        }
        return available;
    }

    private List<TermStudentDTO> enrolledSnapshot(UUID termId) {
        List<TermStudentDTO> enrolled = new ArrayList<>();
        for (TermEnrollment enrollment : termEnrollmentRepository.findByTermIdWithUser(termId)) {
            UserAccount user = enrollment.getUser();
            if (user != null && isStudent(user) && user.getIsActive()) {
                enrolled.add(toStudent(user));
            }
        }
        return enrolled;
    }

    private void requireTermExists(UUID termId) {
        if (!termRepository.existsById(termId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter not found");
        }
    }

    private Term requireTerm(UUID termId) {
        return termRepository.findById(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter not found"));
    }

    static String formatQuarterLabel(int termNumber) {
        if (termNumber == 4) {
            return "Quarter 4 (Summer Quarter)";
        }
        return "Quarter " + termNumber;
    }

    public static String buildTermLabel(Term term) {
        if (term == null) {
            return null;
        }
        String yearLabel = term.getAcademicYear() != null ? term.getAcademicYear().getYearLabel() : "";
        return yearLabel + " — " + formatQuarterLabel(term.getTermNumber());
    }

    private TermSummaryDTO toSummary(Term term) {
        return toSummary(term, (int) termEnrollmentRepository.countByTerm_Id(term.getId()));
    }

    private TermSummaryDTO toSummary(Term term, int studentCount) {
        String yearLabel = term.getAcademicYear() != null ? term.getAcademicYear().getYearLabel() : "";
        return new TermSummaryDTO(
                term.getId(),
                yearLabel + " — " + formatQuarterLabel(term.getTermNumber()),
                term.getEndDate(),
                yearLabel,
                term.getTermNumber(),
                term.isCurrent(),
                studentCount);
    }

    private Map<UUID, Integer> enrollmentCountsByTerm() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : termEnrollmentRepository.countGroupedByTermId()) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private TermStudentDTO toStudent(UserAccount user) {
        return new TermStudentDTO(
                user.getId(),
                user.getFullName() != null ? user.getFullName() : "",
                user.getStudentCode() != null ? user.getStudentCode() : "",
                user.getEmail() != null ? user.getEmail() : "",
                user.getIsActive());
    }

    private String normalizeStudentCode(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.matches("\\d+\\.0+")) {
            return text.substring(0, text.indexOf('.'));
        }
        return text;
    }

    private String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private String normalizeFullName(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private Map<String, UserAccount> loadUsersByStudentCode(Set<String> codes) {
        Map<String, UserAccount> byCode = new HashMap<>();
        if (codes.isEmpty()) {
            return byCode;
        }
        for (UserAccount user : userAccountRepository.findByStudentCodeLowerIn(List.copyOf(codes))) {
            if (user.getStudentCode() != null) {
                byCode.put(user.getStudentCode().toLowerCase(Locale.ROOT), user);
            }
        }
        return byCode;
    }

    private Map<String, UserAccount> loadUsersByEmail(Set<String> emails) {
        Map<String, UserAccount> byEmail = new HashMap<>();
        if (emails.isEmpty()) {
            return byEmail;
        }
        for (UserAccount user : userAccountRepository.findByEmailLowerIn(List.copyOf(emails))) {
            if (user.getEmail() != null) {
                byEmail.put(user.getEmail().toLowerCase(Locale.ROOT), user);
            }
        }
        return byEmail;
    }

    private UserAccount resolveExistingStudent(
            String studentCode,
            String email,
            Map<String, UserAccount> byCode,
            Map<String, UserAccount> byEmail) {
        if (!studentCode.isEmpty()) {
            UserAccount byIrn = byCode.get(studentCode.toLowerCase(Locale.ROOT));
            if (byIrn != null) {
                return byIrn;
            }
        }
        if (!email.isEmpty()) {
            return byEmail.get(email.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private void recordNotice(
            List<ImportUnmatchedStudent> target,
            List<String> unmatched,
            String fullName,
            String studentCode,
            String email,
            String reason) {
        if (target.size() >= 25) {
            return;
        }
        String codeValue = studentCode == null ? "" : studentCode.trim();
        String emailValue = email == null ? "" : email.trim();
        String nameValue = fullName == null ? "" : fullName.trim();
        String displayName = firstNonBlank(nameValue, firstNonBlank(codeValue, firstNonBlank(emailValue, "Unknown name")));
        target.add(new ImportUnmatchedStudent(
                nameValue.isBlank() ? displayName : nameValue,
                codeValue.isBlank() ? null : codeValue,
                emailValue.isBlank() ? null : emailValue,
                reason));
        if (unmatched != null) {
            unmatched.add(displayName + " — " + reason);
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private boolean isStudent(UserAccount user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT))
                .anyMatch(name -> "STUDENT".equals(name));
    }
}
