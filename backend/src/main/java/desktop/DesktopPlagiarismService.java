package com.eiu.capstone.backend.desktop;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.DTO.plagiarism.LabPlagiarismReportDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismFlagsDTO;
import com.eiu.capstone.backend.DTO.plagiarism.PlagiarismInvestigationDTO;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.plagiarism.PlagiarismService;
import com.eiu.capstone.backend.plagiarism.PlagiarismSignals;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("desktop")
public class DesktopPlagiarismService extends PlagiarismService {

    public DesktopPlagiarismService(
            SubmissionPlagiarismFingerprintRepository fingerprintRepository,
            SubmissionPlagiarismMatchRepository matchRepository,
            LabSubmissionRepository labSubmissionRepository,
            ObjectMapper objectMapper) {
        super(fingerprintRepository, matchRepository, labSubmissionRepository, objectMapper);
    }

    @Override
    public PlagiarismSignals snapshotSignals(List<MultipartFile> files) {
        return PlagiarismSignals.empty();
    }

    @Override
    public void inspectUpload(LabSubmission submission, PlagiarismSignals signals) {
        // offline practice — no plagiarism pipeline
    }

    @Override
    public void inspectUpload(LabSubmission submission, List<MultipartFile> files) {
        // no-op
    }

    @Override
    public void reevaluateLabMatches(UUID labId) {
        // no-op
    }

    @Override
    public Set<UUID> flaggedSubmissionIds(UUID labId, Collection<UUID> submissionIds) {
        return Set.of();
    }

    @Override
    public PlagiarismFlagsDTO lecturerFlags() {
        return new PlagiarismFlagsDTO(List.of(), Map.of(), Map.of(), Map.of());
    }

    @Override
    public Map<UUID, String> studentRolesForLab(UUID labId) {
        return Map.of();
    }

    @Override
    public long countFlaggedStudentsForLab(UUID labId) {
        return 0L;
    }

    @Override
    public LabPlagiarismReportDTO reportForLab(UUID labId) {
        return new LabPlagiarismReportDTO(labId, List.of());
    }

    @Override
    public PlagiarismInvestigationDTO investigationForStudent(UUID labId, UUID focusStudentId) {
        return new PlagiarismInvestigationDTO(
                labId, focusStudentId, null, null, null, List.of(), List.of(), null);
    }
}
