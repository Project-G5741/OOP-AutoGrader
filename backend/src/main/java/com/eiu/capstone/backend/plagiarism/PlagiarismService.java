package com.eiu.capstone.backend.plagiarism;

import java.math.BigDecimal;
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
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismMatchDTO;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionPlagiarismFingerprint;
import com.eiu.capstone.backend.model.SubmissionPlagiarismMatch;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PlagiarismService {

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

        List<SubmissionPlagiarismMatch> matches = new ArrayList<>();
        for (SubmissionPlagiarismFingerprint other : fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)) {
            PlagiarismComparison comparison = PlagiarismComparator.compare(signals, toSignals(other));
            SubmissionPlagiarismMatch match = new SubmissionPlagiarismMatch();
            match.setLabId(labId);
            match.setSubmissionId(submissionId);
            match.setOtherSubmissionId(other.getSubmissionId());
            match.setGitMatch(comparison.gitMatch());
            match.setMetadataMatch(comparison.metadataMatch());
            match.setHashSimilarity(comparison.hashSimilarity());
            match.setFlagged(comparison.flagged());
            matches.add(match);
        }
        if (!matches.isEmpty()) {
            matchRepository.saveAll(matches);
        }
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
        for (SubmissionPlagiarismMatch match : matchRepository.findByLabIdAndFlaggedTrue(labId)) {
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
        List<SubmissionPlagiarismMatch> matches = matchRepository.findByFlaggedTrue();
        Set<UUID> flaggedLabIds = new HashSet<>();
        Map<UUID, Set<UUID>> labsByStudent = new java.util.HashMap<>();
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
        for (SubmissionPlagiarismMatch match : matches) {
            addStudentLab(labsByStudent, submissionsById.get(match.getSubmissionId()), match.getLabId());
            addStudentLab(labsByStudent, submissionsById.get(match.getOtherSubmissionId()), match.getLabId());
        }
        Map<UUID, List<UUID>> flaggedLabsByStudentId = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : labsByStudent.entrySet()) {
            flaggedLabsByStudentId.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new PlagiarismFlagsDTO(List.copyOf(flaggedLabIds), flaggedLabsByStudentId);
    }

    @Transactional(readOnly = true)
    public LabPlagiarismReportDTO reportForLab(UUID labId) {
        List<SubmissionPlagiarismMatch> matches = matchRepository.findByLabIdAndFlaggedTrue(labId);
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
