package com.eiu.capstone.backend.grading;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;

@Component
class GradingResultStore {

    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final SubmissionChallengeResultRepository submissionChallengeResultRepository;

    GradingResultStore(SubmissionFieldResultRepository submissionFieldResultRepository,
                       SubmissionMethodResultRepository submissionMethodResultRepository,
                       SubmissionConstructorResultRepository submissionConstructorResultRepository,
                       SubmissionChallengeResultRepository submissionChallengeResultRepository) {
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.submissionChallengeResultRepository = submissionChallengeResultRepository;
    }

    @Transactional(readOnly = true)
    GradingService.ExistingResults loadExisting(LabSubmission submission) {
        GradingService.ExistingResults existing = new GradingService.ExistingResults();
        existing.fieldResults = submissionFieldResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getField().getId(), r -> r));
        existing.methodResults = submissionMethodResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getMethod().getId(), r -> r));
        existing.constructorResults = submissionConstructorResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getConstructor().getId(), r -> r));
        existing.challengeResults = submissionChallengeResultRepository.findBySubmission(submission)
                .stream().collect(Collectors.toMap(r -> r.getChallenge().getId(), r -> r));
        return existing;
    }

    @Transactional
    void save(GradingService.GradingComputationResult computed) {
        submissionFieldResultRepository.saveAll(computed.fieldResults);
        submissionMethodResultRepository.saveAll(computed.methodResults);
        submissionConstructorResultRepository.saveAll(computed.constructorResults);
        submissionChallengeResultRepository.saveAll(computed.challengeResults);
    }
}
