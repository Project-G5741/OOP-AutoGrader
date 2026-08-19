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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CreateTermRequest;
import com.eiu.capstone.backend.DTO.ImportStudentRow;
import com.eiu.capstone.backend.DTO.ImportStudentsRequest;
import com.eiu.capstone.backend.DTO.ImportStudentsResult;
import com.eiu.capstone.backend.DTO.TermRosterDTO;
import com.eiu.capstone.backend.DTO.TermStudentDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TermEnrollment;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
public class TermService {

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;

    public TermService(TermRepository termRepository,
                       AcademicYearRepository academicYearRepository,
                       TermEnrollmentRepository termEnrollmentRepository,
                       UserAccountRepository userAccountRepository) {
        this.termRepository = termRepository;
        this.academicYearRepository = academicYearRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<TermSummaryDTO> listTerms() {
        Map<UUID, Integer> counts = enrollmentCountsByTerm();
        return termRepository.findAllWithAcademicYear().stream()
                .sorted(Comparator
                        .comparing((Term t) -> t.getAcademicYear().getYearLabel()).reversed()
                        .thenComparing(Term::getTermNumber))
                .map(term -> toSummary(term, counts.getOrDefault(term.getId(), 0)))
                .toList();
    }

    @Transactional
    public TermSummaryDTO createTerm(CreateTermRequest request) {
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
                    "That term already exists for " + yearLabel);
        }
        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setCurrent(false);
        term = termRepository.save(term);
        if (request.setCurrent()) {
            return setCurrentTerm(term.getId());
        }
        return toSummary(term);
    }

    @Transactional
    public TermSummaryDTO setCurrentTerm(UUID termId) {
        Term target = termRepository.findById(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        termRepository.clearOtherCurrent(termId);
        target.setCurrent(true);
        return toSummary(termRepository.save(target));
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
            if (isStudent(user)) {
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
        Set<UUID> seenUsers = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (ImportStudentRow row : request.rows()) {
            String studentCode = normalizeStudentCode(row == null ? null : row.studentCode());
            if (!studentCode.isEmpty()) {
                codes.add(studentCode.toLowerCase(Locale.ROOT));
            }
        }
        Map<String, UserAccount> byCode = new HashMap<>();
        if (!codes.isEmpty()) {
            for (UserAccount user : userAccountRepository.findByStudentCodeLowerIn(List.copyOf(codes))) {
                if (user.getStudentCode() != null) {
                    byCode.put(user.getStudentCode().toLowerCase(Locale.ROOT), user);
                }
            }
        }
        Set<UUID> alreadyEnrolled = new HashSet<>(termEnrollmentRepository.findUserIdsByTermId(term.getId()));
        List<TermEnrollment> toSave = new ArrayList<>();

        for (ImportStudentRow row : request.rows()) {
            String studentCode = normalizeStudentCode(row == null ? null : row.studentCode());
            String email = normalizeEmail(row == null ? null : row.email());
            if (studentCode.isEmpty() || email.isEmpty()) {
                skipped++;
                continue;
            }
            UserAccount user = byCode.get(studentCode.toLowerCase(Locale.ROOT));
            if (user == null || !email.equalsIgnoreCase(normalizeEmail(user.getEmail()))) {
                notFound++;
                if (unmatched.size() < 25) {
                    unmatched.add(studentCode + " / " + email);
                }
                continue;
            }
            if (!user.getIsActive() || !isStudent(user)) {
                skipped++;
                continue;
            }
            if (!seenUsers.add(user.getId()) || alreadyEnrolled.contains(user.getId())) {
                alreadyInTerm++;
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
                enrolledSnapshot(termId));
    }

    @Transactional
    public void removeStudent(UUID termId, UUID studentId) {
        requireTerm(termId);
        TermEnrollment enrollment = termEnrollmentRepository.findByUser_IdAndTerm_Id(studentId, termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not in this term"));
        termEnrollmentRepository.delete(enrollment);
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
            if (user != null && isStudent(user)) {
                enrolled.add(toStudent(user));
            }
        }
        return enrolled;
    }

    private void requireTermExists(UUID termId) {
        if (!termRepository.existsById(termId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found");
        }
    }

    private Term requireTerm(UUID termId) {
        return termRepository.findById(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
    }

    private TermSummaryDTO toSummary(Term term) {
        return toSummary(term, (int) termEnrollmentRepository.countByTerm_Id(term.getId()));
    }

    private TermSummaryDTO toSummary(Term term, int studentCount) {
        String yearLabel = term.getAcademicYear() != null ? term.getAcademicYear().getYearLabel() : "";
        return new TermSummaryDTO(
                term.getId(),
                yearLabel + " — Term " + term.getTermNumber(),
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

    private boolean isStudent(UserAccount user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(role -> role.getName() == null ? "" : role.getName().trim().toUpperCase(Locale.ROOT))
                .anyMatch(name -> "STUDENT".equals(name));
    }
}
