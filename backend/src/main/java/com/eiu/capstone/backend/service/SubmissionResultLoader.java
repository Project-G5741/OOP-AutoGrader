package com.eiu.capstone.backend.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.model.SubmissionRelationResult;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;

@Service
public class SubmissionResultLoader {

    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final SubmissionRelationResultRepository submissionRelationResultRepository;

    public SubmissionResultLoader(SubmissionFieldResultRepository submissionFieldResultRepository,
                                  SubmissionMethodResultRepository submissionMethodResultRepository,
                                  SubmissionConstructorResultRepository submissionConstructorResultRepository,
                                  SubmissionRelationResultRepository submissionRelationResultRepository) {
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.submissionRelationResultRepository = submissionRelationResultRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionCorrectIds loadCorrectIds(UUID submissionId) {
        Set<UUID> fieldIds = new HashSet<>();
        for (SubmissionFieldResult result : submissionFieldResultRepository.findBySubmission_IdWithField(submissionId)) {
            if (result.isCorrect()) {
                fieldIds.add(result.getField().getId());
            }
        }

        Set<UUID> methodIds = new HashSet<>();
        for (SubmissionMethodResult result : submissionMethodResultRepository.findBySubmission_IdWithMethod(submissionId)) {
            if (result.isCorrect()) {
                methodIds.add(result.getMethod().getId());
            }
        }

        Set<UUID> constructorIds = new HashSet<>();
        for (SubmissionConstructorResult result : submissionConstructorResultRepository.findBySubmission_IdWithConstructor(submissionId)) {
            if (result.isCorrect()) {
                constructorIds.add(result.getConstructor().getId());
            }
        }

        Set<UUID> relationIds = new HashSet<>();
        for (SubmissionRelationResult result : submissionRelationResultRepository.findBySubmission_IdWithRelation(submissionId)) {
            if (result.isCorrect()) {
                relationIds.add(result.getClassRelation().getId());
            }
        }

        return new SubmissionCorrectIds(fieldIds, methodIds, constructorIds, relationIds);
    }
}
