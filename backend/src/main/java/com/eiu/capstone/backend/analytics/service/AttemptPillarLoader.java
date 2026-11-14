package com.eiu.capstone.backend.analytics.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.analytics.mapper.AnalyticsMapper;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.ChallengeSource;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.Recovered;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.Sources;
import com.eiu.capstone.backend.grading.LabPillarScores;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Fills pillar scores for attempts that predate the stored columns, from the
 * ticks kept on that same attempt.
 */
@Service
public class AttemptPillarLoader {

    @PersistenceContext
    private EntityManager entityManager;

    private final LabRubricCache labRubricCache;

    public AttemptPillarLoader(LabRubricCache labRubricCache) {
        this.labRubricCache = labRubricCache;
    }

    public Map<UUID, LabPillarScores> labScores(UUID labId, Collection<UUID> submissionIds) {
        Map<UUID, Recovered> recovered = recoverAll(labId, submissionIds);
        Map<UUID, LabPillarScores> scores = new HashMap<>();
        recovered.forEach((id, value) -> scores.put(id, value.lab()));
        return scores;
    }

    public Map<UUID, LabPillarScores> challengeScores(UUID labId, UUID challengeId, Collection<UUID> submissionIds) {
        Map<UUID, Recovered> recovered = recoverAll(labId, submissionIds);
        Map<UUID, LabPillarScores> scores = new HashMap<>();
        for (Map.Entry<UUID, Recovered> entry : recovered.entrySet()) {
            LabPillarScores pillar = entry.getValue().byChallenge().get(challengeId);
            if (pillar != null) {
                scores.put(entry.getKey(), pillar);
            }
        }
        return scores;
    }

    private Map<UUID, Recovered> recoverAll(UUID labId, Collection<UUID> submissionIds) {
        List<UUID> ids = new ArrayList<>();
        if (submissionIds != null) {
            for (UUID id : submissionIds) {
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        if (labId == null || ids.isEmpty()) {
            return Map.of();
        }
        LabRubricSnapshot rubric;
        try {
            rubric = labRubricCache.get(labId);
        } catch (RuntimeException ex) {
            return Map.of();
        }
        if (rubric == null) {
            return Map.of();
        }
        Map<UUID, List<ChallengeSource>> challenges = new HashMap<>();
        for (Object[] row : query("""
                SELECT scr.submission_id, scr.challenge_id, scr.score, CAST(scr.mmd_outcomes AS VARCHAR)
                FROM submission_challenge_result scr
                WHERE scr.submission_id IN (:ids)
                """, ids)) {
            UUID submissionId = AnalyticsMapper.toUuid(row[0]);
            UUID challengeId = AnalyticsMapper.toUuid(row[1]);
            if (submissionId == null || challengeId == null) {
                continue;
            }
            challenges.computeIfAbsent(submissionId, ignored -> new ArrayList<>())
                    .add(new ChallengeSource(challengeId, AnalyticsMapper.toBigDecimal(row[2]), AnalyticsMapper.toString(row[3])));
        }
        Map<UUID, Map<UUID, Boolean>> fields = flags("submission_field_result", "field_id", ids);
        Map<UUID, Map<UUID, Boolean>> methods = flags("submission_method_result", "method_id", ids);
        Map<UUID, Map<UUID, Boolean>> constructors = flags("submission_constructor_result", "constructor_id", ids);
        Map<UUID, Map<UUID, String>> testcases = new HashMap<>();
        for (Object[] row : query("""
                SELECT submission_id, testcase_id, result
                FROM submission_testcase_result
                WHERE submission_id IN (:ids)
                """, ids)) {
            UUID submissionId = AnalyticsMapper.toUuid(row[0]);
            UUID testcaseId = AnalyticsMapper.toUuid(row[1]);
            if (submissionId == null || testcaseId == null) {
                continue;
            }
            testcases.computeIfAbsent(submissionId, ignored -> new HashMap<>())
                    .put(testcaseId, AnalyticsMapper.toString(row[2]));
        }
        Map<UUID, Recovered> recovered = new HashMap<>();
        for (UUID submissionId : ids) {
            if (!challenges.containsKey(submissionId)) {
                continue;
            }
            recovered.put(submissionId, AttemptPillarRecovery.recover(rubric, new Sources(
                    challenges.get(submissionId),
                    fields.getOrDefault(submissionId, Map.of()),
                    methods.getOrDefault(submissionId, Map.of()),
                    constructors.getOrDefault(submissionId, Map.of()),
                    testcases.getOrDefault(submissionId, Map.of()))));
        }
        return recovered;
    }

    private Map<UUID, Map<UUID, Boolean>> flags(String table, String column, List<UUID> ids) {
        Map<UUID, Map<UUID, Boolean>> grouped = new HashMap<>();
        for (Object[] row : query("""
                SELECT submission_id, %s, is_correct
                FROM %s
                WHERE submission_id IN (:ids)
                """.formatted(column, table), ids)) {
            UUID submissionId = AnalyticsMapper.toUuid(row[0]);
            UUID memberId = AnalyticsMapper.toUuid(row[1]);
            if (submissionId == null || memberId == null) {
                continue;
            }
            grouped.computeIfAbsent(submissionId, ignored -> new HashMap<>())
                    .put(memberId, AnalyticsMapper.toBoolean(row[2]));
        }
        return grouped;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> query(String sql, List<UUID> ids) {
        return entityManager.createNativeQuery(sql)
                .setParameter("ids", ids)
                .getResultList();
    }
}
