package com.eiu.capstone.backend.grading;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;

@Component
class GradingResultStore {

    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final SubmissionRelationResultRepository submissionRelationResultRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;

    GradingResultStore(SubmissionFieldResultRepository submissionFieldResultRepository,
                       SubmissionMethodResultRepository submissionMethodResultRepository,
                       SubmissionConstructorResultRepository submissionConstructorResultRepository,
                       SubmissionRelationResultRepository submissionRelationResultRepository,
                       SubmissionChallengeResultRepository submissionChallengeResultRepository) {
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.submissionRelationResultRepository = submissionRelationResultRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
    }

    @Transactional(readOnly = true)
    GradingService.ExistingResults loadExisting(LabSubmission submission) {
        UUID submissionId = submission.getId();
        GradingService.ExistingResults existing = new GradingService.ExistingResults();
        existing.fieldResults = submissionFieldResultRepository.findBySubmission_IdWithField(submissionId)
                .stream().collect(Collectors.toMap(r -> r.getField().getId(), r -> r));
        existing.methodResults = submissionMethodResultRepository.findBySubmission_IdWithMethod(submissionId)
                .stream().collect(Collectors.toMap(r -> r.getMethod().getId(), r -> r));
        existing.constructorResults = submissionConstructorResultRepository.findBySubmission_IdWithConstructor(submissionId)
                .stream().collect(Collectors.toMap(r -> r.getConstructor().getId(), r -> r));
        existing.relationResults = submissionRelationResultRepository.findBySubmission_IdWithRelation(submissionId)
                .stream().collect(Collectors.toMap(r -> r.getClassRelation().getId(), r -> r));
        existing.challengeResults = submissionChallengeResultRepository.findBySubmission_IdWithChallenge(submissionId)
                .stream().collect(Collectors.toMap(r -> r.getChallenge().getId(), r -> r));
        return existing;
    }

    @Transactional
    void save(GradingService.GradingComputationResult computed) {
        submissionFieldResultRepository.saveAll(computed.fieldResults);
        submissionMethodResultRepository.saveAll(computed.methodResults);
        submissionConstructorResultRepository.saveAll(computed.constructorResults);
        submissionRelationResultRepository.saveAll(computed.relationResults);
        submissionChallengeResultRepository.saveAll(computed.challengeResults);
    }
}
