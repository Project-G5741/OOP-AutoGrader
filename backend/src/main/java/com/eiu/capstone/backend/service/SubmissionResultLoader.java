package com.eiu.capstone.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.repository.SubmissionResultReadRepository;

@Service
public class SubmissionResultLoader {

    private final SubmissionResultReadRepository submissionResultReadRepository;

    public SubmissionResultLoader(SubmissionResultReadRepository submissionResultReadRepository) {
        this.submissionResultReadRepository = submissionResultReadRepository;
    }

    @Transactional(readOnly = true)
    public SubmissionCorrectIds loadCorrectIds(UUID submissionId) {
        var grouped = submissionResultReadRepository.findCorrectIdsByType(submissionId);
        return new SubmissionCorrectIds(
                toSet(grouped.get("field")),
                toSet(grouped.get("method")),
                toSet(grouped.get("constructor")),
                toSet(grouped.get("relation")));
    }

    private static Set<UUID> toSet(List<UUID> ids) {
        return ids == null ? Set.of() : new HashSet<>(ids);
    }
}
