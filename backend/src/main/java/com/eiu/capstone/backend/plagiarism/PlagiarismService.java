package com.eiu.capstone.backend.plagiarism;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.DTO.plagiarism.LabPlagiarismReportDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismFlagsDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismInvestigationDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismMatchDTO;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionPlagiarismFingerprint;
import com.eiu.capstone.backend.model.SubmissionPlagiarismMatch;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PlagiarismService {

    public static final String ROLE_ORIGINAL = "ORIGINAL";
    public static final String ROLE_PLAGIARIZER = "PLAGIARIZER";

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final SubmissionPlagiarismFingerprintRepository fingerprintRepository;
    private final SubmissionPlagiarismMatchRepository matchRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final ObjectMapper objectMapper;

    public PlagiarismService(SubmissionPlagiarismFingerprintRepository fingerprintRepository,
                             SubmissionPlagiarismMatchRepository matchRepository,
                             LabSubmissionRepository labSubmissionRepository,
                             ObjectMapper objectMapper) {
        this.fingerprintRepository = fingerprintRepository;
        this.matchRepository = matchRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void inspectUpload(LabSubmission submission, List<MultipartFile> files) {
        if (submission == null || submission.getId() == null || submission.getLab() == null
                || submission.getUser() == null) {
            return;
        }
        UUID submissionId = submission.getId();
        UUID labId = submission.getLab().getId();
        UUID userId = submission.getUser().getId();

        PlagiarismSignals signals = PlagiarismFingerprintExtractor.extract(files);
        SubmissionPlagiarismFingerprint fingerprint = fingerprintRepository.findById(submissionId)
                .orElseGet(SubmissionPlagiarismFingerprint::new);
        fingerprint.setSubmissionId(submissionId);
        fingerprint.setLabId(labId);
        fingerprint.setUserId(userId);
        fingerprint.setGitCommitHashes(writeJson(signals.gitCommitHashes()));
        fingerprint.setMetadataCanonical(signals.metadataCanonical());
        fingerprint.setFileHashes(writeJson(signals.fileHashes()));
        fingerprintRepository.save(fingerprint);

        matchRepository.deleteInvolvingSubmission(submissionId);

        BigDecimal currentScore = submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO;
        BigDecimal priorBest = labSubmissionRepository.bestScoreForUserAndLabExcludingSubmission(
                userId, labId, submissionId);
        if (priorBest == null) {
            priorBest = BigDecimal.ZERO;
        }

        List<SubmissionPlagiarismFingerprint> others =
                fingerprintRepository.findByLabIdAndUserIdNot(labId, userId);
        Set<UUID> otherUserIds = new HashSet<>();
        for (SubmissionPlagiarismFingerprint other : others) {
            if (other.getUserId() != null) {
                otherUserIds.add(other.getUserId());
            }
        }
        Map<UUID, BigDecimal> otherBestByUserId = bestScoresForLabUsers(labId, otherUserIds);

        List<SubmissionPlagiarismMatch> matches = new ArrayList<>();
        for (SubmissionPlagiarismFingerprint other : others) {
            PlagiarismComparison comparison = PlagiarismComparator.compare(signals, toSignals(other));
            BigDecimal otherBest = otherBestByUserId.getOrDefault(other.getUserId(), BigDecimal.ZERO);
            // Use prior best (exclude current): first-time copy that scores 100 still flags (prior=0 < peer).
            // Already-proven ability (prior >= peer best) does not flag. Zero-score attempts never flag.
            boolean scoreGate = scoreGateAllowsFlag(priorBest, otherBest, currentScore);
            SubmissionPlagiarismMatch match = new SubmissionPlagiarismMatch();
            match.setLabId(labId);
            match.setSubmissionId(submissionId);
            match.setOtherSubmissionId(other.getSubmissionId());
            match.setGitMatch(comparison.gitMatch());
            match.setMetadataMatch(comparison.metadataMatch());
            match.setHashSimilarity(comparison.hashSimilarity());
            match.setFlagged(comparison.flagged() && scoreGate);
            matches.add(match);
        }
        if (!matches.isEmpty()) {
            matchRepository.saveAll(matches);
        }
        reevaluateLabMatches(labId);
    }

    /**
     * Flag when content matches and the uploader had not already matched/exceeded the peer's lab best
     * before this attempt. {@code priorBest} excludes the current attempt so a first submission that
     * copies a 100% solution still flags (prior=0 &lt; peer). Zero-score attempts never flag.
     */
    static boolean scoreGateAllowsFlag(BigDecimal priorBest, BigDecimal otherBest, BigDecimal currentAttemptScore) {
        BigDecimal current = currentAttemptScore == null ? BigDecimal.ZERO : currentAttemptScore;
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal prior = priorBest == null ? BigDecimal.ZERO : priorBest;
        BigDecimal other = otherBest == null ? BigDecimal.ZERO : otherBest;
        return prior.compareTo(other) < 0;
    }

    static boolean isContentMatch(SubmissionPlagiarismMatch match) {
        if (match == null) {
            return false;
        }
        if (match.isGitMatch() || match.isMetadataMatch()) {
            return true;
        }
        BigDecimal similarity = match.getHashSimilarity() == null ? BigDecimal.ZERO : match.getHashSimilarity();
        return similarity.compareTo(PlagiarismComparator.HASH_FLAG_THRESHOLD) > 0;
    }

    @Transactional
    public void reevaluateLabMatches(UUID labId) {
        if (labId == null) {
            return;
        }
        List<SubmissionPlagiarismMatch> matches = matchRepository.findByLabId(labId);
        if (matches.isEmpty()) {
            return;
        }
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Map<UUID, LabSubmission> submissionsById = new java.util.HashMap<>();
        for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
            submissionsById.put(submission.getId(), submission);
        }
        Set<UUID> userIds = new HashSet<>();
        for (LabSubmission submission : submissionsById.values()) {
            UUID sid = studentId(submission);
            if (sid != null) {
                userIds.add(sid);
            }
        }
        // Batch: all attempts for these users in this lab (avoids N+1 prior-best queries).
        Map<UUID, List<LabSubmission>> attemptsByUser = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            for (LabSubmission attempt : labSubmissionRepository.findByLabIdAndUserIdIn(labId, userIds)) {
                UUID sid = studentId(attempt);
                if (sid == null) {
                    continue;
                }
                attemptsByUser.computeIfAbsent(sid, ignored -> new ArrayList<>()).add(attempt);
            }
        }
        Map<UUID, BigDecimal> bestByUser = bestScoresFromAttempts(attemptsByUser);
        List<SubmissionPlagiarismMatch> dirty = new ArrayList<>();
        for (SubmissionPlagiarismMatch match : matches) {
            LabSubmission uploader = submissionsById.get(match.getSubmissionId());
            LabSubmission other = submissionsById.get(match.getOtherSubmissionId());
            UUID uploaderUser = studentId(uploader);
            UUID otherUser = studentId(other);
            if (uploaderUser == null || otherUser == null || uploader == null) {
                continue;
            }
            BigDecimal priorBest = bestScoreExcluding(
                    attemptsByUser.getOrDefault(uploaderUser, List.of()), match.getSubmissionId());
            BigDecimal currentScore = uploader.getScore() != null ? uploader.getScore() : BigDecimal.ZERO;
            BigDecimal otherBest = bestByUser.getOrDefault(otherUser, BigDecimal.ZERO);
            // Only the uploader's latest attempt can stay flagged — a later original submit clears older copy flags.
            boolean shouldFlag = isLatestAttemptForUser(uploader, attemptsByUser)
                    && isContentMatch(match)
                    && scoreGateAllowsFlag(priorBest, otherBest, currentScore);
            if (match.isFlagged() != shouldFlag) {
                match.setFlagged(shouldFlag);
                dirty.add(match);
            }
        }
        if (!dirty.isEmpty()) {
            matchRepository.saveAll(dirty);
        }
    }

    /**
     * Flagged matches that still count for UI/stats: uploader side must be that student's
     * latest lab attempt (so a later clean submit clears plagiarism status).
     */
    private List<SubmissionPlagiarismMatch> activeFlaggedMatches(List<SubmissionPlagiarismMatch> flaggedMatches) {
        if (flaggedMatches == null || flaggedMatches.isEmpty()) {
            return List.of();
        }
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : flaggedMatches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Map<UUID, LabSubmission> submissionsById = new java.util.HashMap<>();
        if (!submissionIds.isEmpty()) {
            for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
                submissionsById.put(submission.getId(), submission);
            }
        }
        Map<UUID, Set<UUID>> userIdsByLab = new java.util.HashMap<>();
        for (SubmissionPlagiarismMatch match : flaggedMatches) {
            if (match.getLabId() == null) {
                continue;
            }
            LabSubmission uploader = submissionsById.get(match.getSubmissionId());
            LabSubmission other = submissionsById.get(match.getOtherSubmissionId());
            UUID uploaderUser = studentId(uploader);
            UUID otherUser = studentId(other);
            if (uploaderUser != null) {
                userIdsByLab.computeIfAbsent(match.getLabId(), ignored -> new HashSet<>()).add(uploaderUser);
            }
            if (otherUser != null) {
                userIdsByLab.computeIfAbsent(match.getLabId(), ignored -> new HashSet<>()).add(otherUser);
            }
        }
        Map<UUID, Map<UUID, UUID>> latestSubmissionByLabAndUser = new java.util.HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : userIdsByLab.entrySet()) {
            UUID labId = entry.getKey();
            Map<UUID, List<LabSubmission>> attemptsByUser = new java.util.HashMap<>();
            for (LabSubmission attempt : labSubmissionRepository.findByLabIdAndUserIdIn(labId, entry.getValue())) {
                UUID sid = studentId(attempt);
                if (sid == null) {
                    continue;
                }
                attemptsByUser.computeIfAbsent(sid, ignored -> new ArrayList<>()).add(attempt);
            }
            Map<UUID, UUID> latestByUser = new java.util.HashMap<>();
            for (Map.Entry<UUID, List<LabSubmission>> userEntry : attemptsByUser.entrySet()) {
                UUID latestId = latestSubmissionId(userEntry.getValue());
                if (latestId != null) {
                    latestByUser.put(userEntry.getKey(), latestId);
                }
            }
            latestSubmissionByLabAndUser.put(labId, latestByUser);
        }
        List<SubmissionPlagiarismMatch> active = new ArrayList<>();
        for (SubmissionPlagiarismMatch match : flaggedMatches) {
            LabSubmission uploader = submissionsById.get(match.getSubmissionId());
            UUID uploaderUser = studentId(uploader);
            if (uploaderUser == null || match.getLabId() == null || match.getSubmissionId() == null) {
                continue;
            }
            UUID latestId = latestSubmissionByLabAndUser
                    .getOrDefault(match.getLabId(), Map.of())
                    .get(uploaderUser);
            if (match.getSubmissionId().equals(latestId)) {
                active.add(match);
            }
        }
        return active;
    }

    private static boolean isLatestAttemptForUser(
            LabSubmission uploader,
            Map<UUID, List<LabSubmission>> attemptsByUser) {
        UUID userId = studentId(uploader);
        if (userId == null || uploader == null || uploader.getId() == null) {
            return false;
        }
        UUID latestId = latestSubmissionId(attemptsByUser.getOrDefault(userId, List.of()));
        return uploader.getId().equals(latestId);
    }

    private static UUID latestSubmissionId(List<LabSubmission> attempts) {
        LabSubmission latest = null;
        for (LabSubmission attempt : attempts) {
            if (attempt == null || attempt.getId() == null) {
                continue;
            }
            if (latest == null || isLaterAttempt(attempt, latest)) {
                latest = attempt;
            }
        }
        return latest == null ? null : latest.getId();
    }

    private static boolean isLaterAttempt(LabSubmission candidate, LabSubmission current) {
        Integer candidateAttempt = candidate.getAttemptNumber();
        Integer currentAttempt = current.getAttemptNumber();
        if (candidateAttempt != null && currentAttempt != null && !candidateAttempt.equals(currentAttempt)) {
            return candidateAttempt > currentAttempt;
        }
        OffsetDateTime candidateAt = candidate.getSubmittedAt();
        OffsetDateTime currentAt = current.getSubmittedAt();
        if (candidateAt != null && currentAt != null && !candidateAt.equals(currentAt)) {
            return candidateAt.isAfter(currentAt);
        }
        if (candidate.getId() == null || current.getId() == null) {
            return false;
        }
        return candidate.getId().compareTo(current.getId()) > 0;
    }

    private static Map<UUID, BigDecimal> bestScoresFromAttempts(Map<UUID, List<LabSubmission>> attemptsByUser) {
        Map<UUID, BigDecimal> bestByUser = new java.util.HashMap<>();
        for (Map.Entry<UUID, List<LabSubmission>> entry : attemptsByUser.entrySet()) {
            BigDecimal best = BigDecimal.ZERO;
            for (LabSubmission attempt : entry.getValue()) {
                if (attempt.getScore() != null && attempt.getScore().compareTo(best) > 0) {
                    best = attempt.getScore();
                }
            }
            bestByUser.put(entry.getKey(), best);
        }
        return bestByUser;
    }

    private static BigDecimal bestScoreExcluding(List<LabSubmission> attempts, UUID excludeSubmissionId) {
        BigDecimal best = BigDecimal.ZERO;
        for (LabSubmission attempt : attempts) {
            if (attempt.getId() == null || attempt.getId().equals(excludeSubmissionId)) {
                continue;
            }
            if (attempt.getScore() != null && attempt.getScore().compareTo(best) > 0) {
                best = attempt.getScore();
            }
        }
        return best;
    }

    private Map<UUID, BigDecimal> bestScoresForLabUsers(UUID labId, Collection<UUID> userIds) {
        Map<UUID, BigDecimal> bestByUser = new java.util.HashMap<>();
        if (labId == null || userIds == null) {
            return bestByUser;
        }
        for (UUID userId : userIds) {
            if (userId == null) {
                continue;
            }
            BigDecimal best = labSubmissionRepository.bestScoreForUserAndLab(userId, labId);
            bestByUser.put(userId, best == null ? BigDecimal.ZERO : best);
        }
        return bestByUser;
    }

    @Transactional(readOnly = true)
    public Set<UUID> flaggedSubmissionIds(UUID labId, Collection<UUID> submissionIds) {
        if (labId == null || submissionIds == null || submissionIds.isEmpty()) {
            return Set.of();
        }
        Set<UUID> requested = new HashSet<>();
        for (UUID id : submissionIds) {
            if (id != null) {
                requested.add(id);
            }
        }
        if (requested.isEmpty()) {
            return Set.of();
        }
        Set<UUID> flagged = new HashSet<>();
        for (SubmissionPlagiarismMatch match : activeFlaggedMatches(matchRepository.findByLabIdAndFlaggedTrue(labId))) {
            if (requested.contains(match.getSubmissionId())) {
                flagged.add(match.getSubmissionId());
            }
            if (requested.contains(match.getOtherSubmissionId())) {
                flagged.add(match.getOtherSubmissionId());
            }
        }
        return flagged;
    }

    @Transactional(readOnly = true)
    public PlagiarismFlagsDTO lecturerFlags() {
        List<SubmissionPlagiarismMatch> matches = activeFlaggedMatches(matchRepository.findByFlaggedTrue());
        Set<UUID> flaggedLabIds = new HashSet<>();
        Map<UUID, Set<UUID>> labsByStudent = new java.util.HashMap<>();
        Map<UUID, Map<UUID, BigDecimal>> overlapByStudentAndLab = new java.util.HashMap<>();
        Map<UUID, Map<UUID, String>> rolesByStudentAndLab = new java.util.HashMap<>();
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            flaggedLabIds.add(match.getLabId());
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Map<UUID, LabSubmission> submissionsById = new java.util.HashMap<>();
        if (!submissionIds.isEmpty()) {
            for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
                submissionsById.put(submission.getId(), submission);
            }
        }
        Map<UUID, Map<UUID, OffsetDateTime>> earliestByLabAndStudent =
                earliestSubmittedAtByLab(matches, submissionsById);
        for (SubmissionPlagiarismMatch match : matches) {
            LabSubmission left = submissionsById.get(match.getSubmissionId());
            LabSubmission right = submissionsById.get(match.getOtherSubmissionId());
            addStudentLab(labsByStudent, left, match.getLabId());
            addStudentLab(labsByStudent, right, match.getLabId());
            BigDecimal similarity = match.getHashSimilarity() != null ? match.getHashSimilarity() : BigDecimal.ZERO;
            mergeOverlap(overlapByStudentAndLab, left, match.getLabId(), similarity);
            mergeOverlap(overlapByStudentAndLab, right, match.getLabId(), similarity);
            mergeMatchRoles(
                    rolesByStudentAndLab,
                    left,
                    right,
                    match.getLabId(),
                    earliestByLabAndStudent.getOrDefault(match.getLabId(), Map.of()));
        }
        Map<UUID, List<UUID>> flaggedLabsByStudentId = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : labsByStudent.entrySet()) {
            flaggedLabsByStudentId.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        Map<UUID, Map<UUID, BigDecimal>> overlapOut = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, Map<UUID, BigDecimal>> entry : overlapByStudentAndLab.entrySet()) {
            overlapOut.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        Map<UUID, Map<UUID, String>> rolesOut = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, Map<UUID, String>> entry : rolesByStudentAndLab.entrySet()) {
            rolesOut.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return new PlagiarismFlagsDTO(
                List.copyOf(flaggedLabIds),
                flaggedLabsByStudentId,
                Map.copyOf(overlapOut),
                Map.copyOf(rolesOut));
    }

    /**
     * Per-student plagiarism role for a lab: ORIGINAL (victim) or PLAGIARIZER.
     * Roles use each student's earliest submission time in the lab (not the matched attempt),
     * so a later re-upload by the original author does not flip them to plagiarizer.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> studentRolesForLab(UUID labId) {
        if (labId == null) {
            return Map.of();
        }
        List<SubmissionPlagiarismMatch> matches =
                activeFlaggedMatches(matchRepository.findByLabIdAndFlaggedTrue(labId));
        if (matches.isEmpty()) {
            return Map.of();
        }
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Map<UUID, LabSubmission> submissionsById = new java.util.HashMap<>();
        for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
            submissionsById.put(submission.getId(), submission);
        }
        Map<UUID, OffsetDateTime> earliestByStudent = earliestSubmittedAtForLab(labId, submissionsById);
        Map<UUID, String> rolesByStudent = new java.util.HashMap<>();
        for (SubmissionPlagiarismMatch match : matches) {
            LabSubmission left = submissionsById.get(match.getSubmissionId());
            LabSubmission right = submissionsById.get(match.getOtherSubmissionId());
            mergeMatchRolesForStudents(rolesByStudent, left, right, earliestByStudent);
        }
        return Map.copyOf(rolesByStudent);
    }

    /**
     * Unique students involved in at least one flagged match for the lab (either side).
     */
    @Transactional(readOnly = true)
    public long countFlaggedStudentsForLab(UUID labId) {
        if (labId == null) {
            return 0L;
        }
        List<SubmissionPlagiarismMatch> matches =
                activeFlaggedMatches(matchRepository.findByLabIdAndFlaggedTrue(labId));
        if (matches.isEmpty()) {
            return 0L;
        }
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Set<UUID> studentIds = new HashSet<>();
        for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
            if (submission.getUser() != null && submission.getUser().getId() != null) {
                studentIds.add(submission.getUser().getId());
            }
        }
        return studentIds.size();
    }

    @Transactional(readOnly = true)
    public LabPlagiarismReportDTO reportForLab(UUID labId) {
        List<SubmissionPlagiarismMatch> matches =
                activeFlaggedMatches(matchRepository.findByLabIdAndFlaggedTrue(labId));
        List<PlagiarismMatchDTO> rows = new ArrayList<>();
        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        var submissionsById = new java.util.HashMap<UUID, LabSubmission>();
        if (!submissionIds.isEmpty()) {
            for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
                submissionsById.put(submission.getId(), submission);
            }
        }
        for (SubmissionPlagiarismMatch match : matches) {
            LabSubmission left = submissionsById.get(match.getSubmissionId());
            LabSubmission right = submissionsById.get(match.getOtherSubmissionId());
            rows.add(new PlagiarismMatchDTO(
                    match.getSubmissionId(),
                    match.getOtherSubmissionId(),
                    displayName(left),
                    studentCode(left),
                    displayName(right),
                    studentCode(right),
                    match.isGitMatch(),
                    match.isMetadataMatch(),
                    match.getHashSimilarity() == null ? BigDecimal.ZERO : match.getHashSimilarity(),
                    match.isFlagged()));
        }
        return new LabPlagiarismReportDTO(labId, rows);
    }

    @Transactional(readOnly = true)
    public PlagiarismInvestigationDTO investigationForStudent(UUID labId, UUID focusStudentId) {
        if (labId == null || focusStudentId == null) {
            return emptyInvestigation(labId, focusStudentId, "Missing lab or student.");
        }
        List<SubmissionPlagiarismMatch> matches =
                activeFlaggedMatches(matchRepository.findByLabIdAndFlaggedTrue(labId));
        if (matches.isEmpty()) {
            return emptyInvestigation(labId, focusStudentId,
                    "No flagged plagiarism matches found for this student in the lab.");
        }

        Set<UUID> submissionIds = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            submissionIds.add(match.getSubmissionId());
            submissionIds.add(match.getOtherSubmissionId());
        }
        Map<UUID, LabSubmission> submissionsById = new java.util.HashMap<>();
        for (LabSubmission submission : labSubmissionRepository.findAllWithUserByIdIn(submissionIds)) {
            submissionsById.put(submission.getId(), submission);
        }

        Map<UUID, Set<UUID>> adjacency = new java.util.HashMap<>();
        for (SubmissionPlagiarismMatch match : matches) {
            UUID left = studentId(submissionsById.get(match.getSubmissionId()));
            UUID right = studentId(submissionsById.get(match.getOtherSubmissionId()));
            if (left == null || right == null) {
                continue;
            }
            adjacency.computeIfAbsent(left, ignored -> new HashSet<>()).add(right);
            adjacency.computeIfAbsent(right, ignored -> new HashSet<>()).add(left);
        }
        if (!adjacency.containsKey(focusStudentId)) {
            return emptyInvestigation(labId, focusStudentId,
                    "No flagged plagiarism matches found for this student in the lab.");
        }

        Set<UUID> component = connectedComponent(focusStudentId, adjacency);
        Map<UUID, OffsetDateTime> earliestByStudent = earliestSubmittedAtForLab(labId, component);
        Map<UUID, String> rolesByStudent = studentRolesForLab(labId);

        UUID originStudentId = null;
        OffsetDateTime originEarliest = null;
        for (UUID studentId : component) {
            OffsetDateTime at = earliestByStudent.get(studentId);
            if (at == null) {
                continue;
            }
            if (originEarliest == null || at.isBefore(originEarliest)
                    || (at.equals(originEarliest) && (originStudentId == null || studentId.compareTo(originStudentId) < 0))) {
                originEarliest = at;
                originStudentId = studentId;
            }
        }
        if (originStudentId == null) {
            originStudentId = focusStudentId;
        }

        Map<UUID, LabSubmission> representativeByStudent = new java.util.HashMap<>();
        for (LabSubmission submission : submissionsById.values()) {
            UUID sid = studentId(submission);
            if (sid == null || !component.contains(sid)) {
                continue;
            }
            LabSubmission current = representativeByStudent.get(sid);
            if (current == null
                    || (submission.getSubmittedAt() != null
                    && (current.getSubmittedAt() == null
                    || submission.getSubmittedAt().isBefore(current.getSubmittedAt())))) {
                representativeByStudent.put(sid, submission);
            }
        }

        List<PlagiarismInvestigationDTO.PlagiarismLineageNodeDTO> nodes = new ArrayList<>();
        List<UUID> ordered = new ArrayList<>(component);
        ordered.sort((a, b) -> {
            OffsetDateTime aa = earliestByStudent.get(a);
            OffsetDateTime bb = earliestByStudent.get(b);
            if (aa == null && bb == null) {
                return a.compareTo(b);
            }
            if (aa == null) {
                return 1;
            }
            if (bb == null) {
                return -1;
            }
            int cmp = aa.compareTo(bb);
            return cmp != 0 ? cmp : a.compareTo(b);
        });
        for (UUID studentId : ordered) {
            LabSubmission rep = representativeByStudent.get(studentId);
            String role = rolesByStudent.getOrDefault(studentId, ROLE_PLAGIARIZER);
            if (studentId.equals(originStudentId)) {
                role = ROLE_ORIGINAL;
            }
            nodes.add(new PlagiarismInvestigationDTO.PlagiarismLineageNodeDTO(
                    studentId,
                    displayName(rep),
                    studentCode(rep),
                    formatOffset(earliestByStudent.get(studentId)),
                    role,
                    studentId.equals(originStudentId),
                    studentId.equals(focusStudentId)));
        }

        List<PlagiarismInvestigationDTO.PlagiarismLineageEdgeDTO> edges = new ArrayList<>();
        Set<String> edgeKeys = new HashSet<>();
        for (SubmissionPlagiarismMatch match : matches) {
            UUID left = studentId(submissionsById.get(match.getSubmissionId()));
            UUID right = studentId(submissionsById.get(match.getOtherSubmissionId()));
            if (left == null || right == null || !component.contains(left) || !component.contains(right)) {
                continue;
            }
            OffsetDateTime leftAt = earliestByStudent.get(left);
            OffsetDateTime rightAt = earliestByStudent.get(right);
            UUID from;
            UUID to;
            if (leftAt != null && rightAt != null && leftAt.isAfter(rightAt)) {
                from = right;
                to = left;
            } else if (leftAt != null && rightAt != null && rightAt.isAfter(leftAt)) {
                from = left;
                to = right;
            } else if (left.compareTo(right) <= 0) {
                from = left;
                to = right;
            } else {
                from = right;
                to = left;
            }
            String key = from + "->" + to;
            if (!edgeKeys.add(key)) {
                continue;
            }
            edges.add(new PlagiarismInvestigationDTO.PlagiarismLineageEdgeDTO(
                    from,
                    to,
                    match.isGitMatch(),
                    match.isMetadataMatch(),
                    match.getHashSimilarity() == null ? BigDecimal.ZERO : match.getHashSimilarity()));
        }

        LabSubmission originRep = representativeByStudent.get(originStudentId);
        return new PlagiarismInvestigationDTO(
                labId,
                focusStudentId,
                originStudentId,
                displayName(originRep),
                studentCode(originRep),
                nodes,
                edges,
                null);
    }

    private static PlagiarismInvestigationDTO emptyInvestigation(
            UUID labId, UUID focusStudentId, String message) {
        return new PlagiarismInvestigationDTO(
                labId,
                focusStudentId,
                null,
                "",
                "",
                List.of(),
                List.of(),
                message);
    }

    private static Set<UUID> connectedComponent(UUID start, Map<UUID, Set<UUID>> adjacency) {
        Set<UUID> visited = new HashSet<>();
        java.util.ArrayDeque<UUID> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst();
            for (UUID next : adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    private static final java.time.format.DateTimeFormatter DISPLAY_TIME =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static String formatOffset(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return DISPLAY_TIME.format(value.atZoneSameInstant(TimeUtil.VIETNAM_ZONE));
    }

    private PlagiarismSignals toSignals(SubmissionPlagiarismFingerprint fingerprint) {
        return new PlagiarismSignals(
                readJson(fingerprint.getGitCommitHashes()),
                fingerprint.getMetadataCanonical() == null ? "" : fingerprint.getMetadataCanonical(),
                readJson(fingerprint.getFileHashes()));
    }

    private List<String> readJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static void addStudentLab(Map<UUID, Set<UUID>> labsByStudent, LabSubmission submission, UUID labId) {
        if (submission == null || submission.getUser() == null || submission.getUser().getId() == null
                || labId == null) {
            return;
        }
        labsByStudent.computeIfAbsent(submission.getUser().getId(), ignored -> new HashSet<>()).add(labId);
    }

    private static void mergeOverlap(
            Map<UUID, Map<UUID, BigDecimal>> overlapByStudentAndLab,
            LabSubmission submission,
            UUID labId,
            BigDecimal similarity) {
        if (submission == null || submission.getUser() == null || submission.getUser().getId() == null
                || labId == null || similarity == null) {
            return;
        }
        UUID studentId = submission.getUser().getId();
        Map<UUID, BigDecimal> byLab =
                overlapByStudentAndLab.computeIfAbsent(studentId, ignored -> new java.util.HashMap<>());
        BigDecimal current = byLab.get(labId);
        if (current == null || similarity.compareTo(current) > 0) {
            byLab.put(labId, similarity);
        }
    }

    private static void mergeMatchRoles(
            Map<UUID, Map<UUID, String>> rolesByStudentAndLab,
            LabSubmission left,
            LabSubmission right,
            UUID labId,
            Map<UUID, OffsetDateTime> earliestByStudent) {
        if (labId == null) {
            return;
        }
        UUID leftStudent = studentId(left);
        UUID rightStudent = studentId(right);
        if (leftStudent == null || rightStudent == null) {
            return;
        }
        String[] roles = rolesForPair(leftStudent, rightStudent, earliestByStudent);
        mergeStudentLabRole(rolesByStudentAndLab, leftStudent, labId, roles[0]);
        mergeStudentLabRole(rolesByStudentAndLab, rightStudent, labId, roles[1]);
    }

    private static void mergeMatchRolesForStudents(
            Map<UUID, String> rolesByStudent,
            LabSubmission left,
            LabSubmission right,
            Map<UUID, OffsetDateTime> earliestByStudent) {
        UUID leftStudent = studentId(left);
        UUID rightStudent = studentId(right);
        if (leftStudent == null || rightStudent == null) {
            return;
        }
        String[] roles = rolesForPair(leftStudent, rightStudent, earliestByStudent);
        rolesByStudent.put(leftStudent, mergeRole(rolesByStudent.get(leftStudent), roles[0]));
        rolesByStudent.put(rightStudent, mergeRole(rolesByStudent.get(rightStudent), roles[1]));
    }

    /**
     * Earlier first-submit in the lab → ORIGINAL (victim); later first-submit → PLAGIARIZER.
     * Uses lab-level earliest times so a victim's later re-upload does not invert roles.
     */
    private static String[] rolesForPair(
            UUID leftStudent,
            UUID rightStudent,
            Map<UUID, OffsetDateTime> earliestByStudent) {
        OffsetDateTime leftAt = earliestByStudent.get(leftStudent);
        OffsetDateTime rightAt = earliestByStudent.get(rightStudent);
        int order;
        if (leftAt != null && rightAt != null && !leftAt.equals(rightAt)) {
            order = leftAt.isBefore(rightAt) ? -1 : 1;
        } else {
            order = leftStudent.compareTo(rightStudent);
        }
        if (order <= 0) {
            return new String[] {ROLE_ORIGINAL, ROLE_PLAGIARIZER};
        }
        return new String[] {ROLE_PLAGIARIZER, ROLE_ORIGINAL};
    }

    private Map<UUID, Map<UUID, OffsetDateTime>> earliestSubmittedAtByLab(
            List<SubmissionPlagiarismMatch> matches,
            Map<UUID, LabSubmission> submissionsById) {
        Map<UUID, Set<UUID>> studentsByLab = new java.util.HashMap<>();
        for (SubmissionPlagiarismMatch match : matches) {
            UUID labId = match.getLabId();
            if (labId == null) {
                continue;
            }
            Set<UUID> students = studentsByLab.computeIfAbsent(labId, ignored -> new HashSet<>());
            UUID left = studentId(submissionsById.get(match.getSubmissionId()));
            UUID right = studentId(submissionsById.get(match.getOtherSubmissionId()));
            if (left != null) {
                students.add(left);
            }
            if (right != null) {
                students.add(right);
            }
        }
        Map<UUID, Map<UUID, OffsetDateTime>> out = new java.util.HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : studentsByLab.entrySet()) {
            out.put(entry.getKey(), earliestSubmittedAtForLab(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private Map<UUID, OffsetDateTime> earliestSubmittedAtForLab(
            UUID labId,
            Map<UUID, LabSubmission> submissionsById) {
        Set<UUID> userIds = new HashSet<>();
        for (LabSubmission submission : submissionsById.values()) {
            UUID id = studentId(submission);
            if (id != null) {
                userIds.add(id);
            }
        }
        return earliestSubmittedAtForLab(labId, userIds);
    }

    private Map<UUID, OffsetDateTime> earliestSubmittedAtForLab(UUID labId, Collection<UUID> userIds) {
        if (labId == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, OffsetDateTime> earliest = new java.util.HashMap<>();
        for (Object[] row : labSubmissionRepository.findEarliestSubmittedAtByLabAndUserIds(labId, userIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            OffsetDateTime at = toOffsetDateTime(row[1]);
            if (at != null) {
                earliest.put((UUID) row[0], at);
            }
        }
        return earliest;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.time.Instant instant) {
            return OffsetDateTime.ofInstant(instant, TimeUtil.VIETNAM_ZONE);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
        }
        return null;
    }

    private static void mergeStudentLabRole(
            Map<UUID, Map<UUID, String>> rolesByStudentAndLab,
            UUID studentId,
            UUID labId,
            String role) {
        Map<UUID, String> byLab =
                rolesByStudentAndLab.computeIfAbsent(studentId, ignored -> new java.util.HashMap<>());
        byLab.put(labId, mergeRole(byLab.get(labId), role));
    }

    /** Prefer PLAGIARIZER when a student appears on both sides across different matches. */
    static String mergeRole(String existing, String incoming) {
        if (incoming == null) {
            return existing;
        }
        if (existing == null || existing.equals(incoming)) {
            return incoming;
        }
        return ROLE_PLAGIARIZER;
    }

    private static UUID studentId(LabSubmission submission) {
        if (submission == null || submission.getUser() == null) {
            return null;
        }
        return submission.getUser().getId();
    }

    private static String displayName(LabSubmission submission) {
        UserAccount user = submission == null ? null : submission.getUser();
        if (user == null) {
            return "";
        }
        return user.getFullName() != null ? user.getFullName() : "";
    }

    private static String studentCode(LabSubmission submission) {
        UserAccount user = submission == null ? null : submission.getUser();
        if (user == null) {
            return "";
        }
        return user.getStudentCode() != null ? user.getStudentCode() : "";
    }
}
