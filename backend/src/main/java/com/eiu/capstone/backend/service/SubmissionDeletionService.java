package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionDeletionService {

    private final SubmissionPlagiarismMatchRepository plagiarismMatchRepository;
    private final SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository;
    private final SubmissionTestcaseResultRepository submissionTestcaseResultRepository;
    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;
    private final SubmissionRelationResultRepository submissionRelationResultRepository;
    private final LabSubmissionRepository labSubmissionRepository;

    public SubmissionDeletionService(
            SubmissionPlagiarismMatchRepository plagiarismMatchRepository,
            SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository,
            SubmissionTestcaseResultRepository submissionTestcaseResultRepository,
            SubmissionFieldResultRepository submissionFieldResultRepository,
            SubmissionMethodResultRepository submissionMethodResultRepository,
            SubmissionConstructorResultRepository submissionConstructorResultRepository,
            SubmissionChallengeResultRepository submissionChallengeResultRepository,
            SubmissionRelationResultRepository submissionRelationResultRepository,
            LabSubmissionRepository labSubmissionRepository) {
        this.plagiarismMatchRepository = plagiarismMatchRepository;
        this.plagiarismFingerprintRepository = plagiarismFingerprintRepository;
        this.submissionTestcaseResultRepository = submissionTestcaseResultRepository;
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
        this.submissionRelationResultRepository = submissionRelationResultRepository;
        this.labSubmissionRepository = labSubmissionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllSubmissionGraphForUser(UUID userId) {
        if (userId == null) {
            return;
        }
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSubmissionsFully(Collection<UUID> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return;
        }
        List<UUID> ids = new ArrayList<>(submissionIds);
        for (UUID submissionId : ids) {
            plagiarismMatchRepository.deleteInvolvingSubmission(submissionId);
            plagiarismFingerprintRepository.deleteBySubmissionId(submissionId);
        }
        submissionTestcaseResultRepository.deleteAssertionResultsBySubmissionIds(ids);
        submissionTestcaseResultRepository.deleteBySubmissionIds(ids);
        submissionFieldResultRepository.deleteBySubmissionIds(ids);
        submissionMethodResultRepository.deleteBySubmissionIds(ids);
        submissionConstructorResultRepository.deleteBySubmissionIds(ids);
        submissionRelationResultRepository.deleteBySubmissionIds(ids);
        submissionChallengeResultRepository.deleteBySubmissionIds(ids);
        labSubmissionRepository.deleteAllByIdIn(ids);
    }
}
