package com.eiu.capstone.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
public class StudentTermAccessService {

    public record UploadAccess(UserAccount user, Lab lab) {}

    private record CachedAccess(UploadAccess access, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final TermService termService;
    private final LabDeadlineHelper labDeadlineHelper;
    private final UserAccountRepository userAccountRepository;
    private final long accessCacheTtlSeconds;
    private final ConcurrentHashMap<String, CachedAccess> accessCache = new ConcurrentHashMap<>();

    public StudentTermAccessService(TermService termService,
                                    LabDeadlineHelper labDeadlineHelper,
                                    UserAccountRepository userAccountRepository,
                                    @Value("${app.upload.access-cache-ttl-seconds:30}") long accessCacheTtlSeconds) {
        this.termService = termService;
        this.labDeadlineHelper = labDeadlineHelper;
        this.userAccountRepository = userAccountRepository;
        this.accessCacheTtlSeconds = accessCacheTtlSeconds;
    }

    public boolean isInCurrentTerm(UserAccount user) {
        if (user == null || !user.getIsActive()) {
            return false;
        }
        return termService.isInCurrentTerm(user.getId());
    }

    public void requireCanSubmit(UserAccount user, Lab lab) {
        requireStudentLabAccess(user, lab);
    }

    /**
     * Upload-path access: one Neon round-trip for user + lab + term + enrollment.
     * Successful checks are cached briefly. GET /api/labs remembers visible labs after
     * enrollment is proven, and student GET challenges/stats reuse this method, so the
     * first upload after opening the dashboard can skip that round-trip.
     */
    public UploadAccess requireUploadAccess(String email, UUID labId) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        if (labId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        String cacheKey = email.trim().toLowerCase() + '\0' + labId;
        if (accessCacheTtlSeconds > 0) {
            CachedAccess cached = accessCache.get(cacheKey);
            if (cached != null) {
                UploadAccess hit = cached.access();
                if (!cached.expired() && hit.user() != null && hit.user().getIsActive()) {
                    requireLabOpenForStudent(hit.lab());
                    return hit;
                }
                accessCache.remove(cacheKey, cached);
            }
        }
        List<Object[]> rows = userAccountRepository.findUploadAccess(email, labId);
        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0)[0] == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user");
        }
        Object[] row = rows.get(0);
        UserAccount user = (UserAccount) row[0];
        Lab lab = row.length > 1 ? (Lab) row[1] : null;
        Term term = row.length > 2 ? (Term) row[2] : null;
        long enrollmentCount = row.length > 3 && row[3] instanceof Number number
                ? number.longValue()
                : 0L;

        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        if (lab == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found");
        }
        if (term != null && lab.getTerm() == null) {
            lab.setTerm(term);
        }
        if (lab.getTerm() == null || !lab.getTerm().isCurrent()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This lab is not in the current quarter");
        }
        if (enrollmentCount < 1) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not enrolled in the current quarter");
        }
        requireLabOpenForStudent(lab);
        UploadAccess access = new UploadAccess(user, lab);
        rememberSuccessfulAccess(email, labId, access);
        return access;
    }

    /**
     * Dashboard already proved enrollment for these labs. Remember them so the first
     * upload can skip the Neon access round-trip. Deadline openness is still checked
     * with {@code Instant.now()} on the next {@link #requireUploadAccess} hit.
     */
    public void rememberSuccessfulAccess(UserAccount user, Lab lab) {
        if (user == null || !user.getIsActive() || lab == null || lab.getId() == null) {
            return;
        }
        rememberSuccessfulAccess(user.getEmail(), lab.getId(), new UploadAccess(user, lab));
    }

    private void rememberSuccessfulAccess(String email, UUID labId, UploadAccess access) {
        if (accessCacheTtlSeconds <= 0 || access == null || labId == null) {
            return;
        }
        if (email == null || email.isBlank()) {
            return;
        }
        String cacheKey = email.trim().toLowerCase() + '\0' + labId;
        accessCache.put(cacheKey, new CachedAccess(
                access, Instant.now().plusSeconds(accessCacheTtlSeconds)));
    }

    public void requireStudentLabAccess(UserAccount user, Lab lab) {
        if (user == null || !user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        Term current = termService.findCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No current quarter is set"));
        if (!termService.isEnrolled(user.getId(), current.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not enrolled in the current quarter");
        }
        if (lab == null || lab.getTerm() == null || !current.getId().equals(lab.getTerm().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This lab is not in the current quarter");
        }
        requireLabOpenForStudent(lab);
    }

    public void requireLabOpenForStudent(Lab lab) {
        if (!labDeadlineHelper.isOpenForStudentSubmission(
                lab.isStudentVisible(), lab.getReleaseDate(), Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This lab is not available for submission yet");
        }
    }
}
