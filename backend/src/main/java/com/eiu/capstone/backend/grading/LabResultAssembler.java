package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.DTO.ChallengeDetailBundleDTO;
import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.DTO.MmdClassDTO;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.pipeline.MmdPillarGrader;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.model.SubmissionRelationResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.LabChallengeStructureBundle;
import com.eiu.capstone.backend.service.SubmissionCorrectIds;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;

@Component
public class LabResultAssembler {

    private final ClassStructureService classStructureService;
    private final boolean timingLog;

    public LabResultAssembler(ClassStructureService classStructureService,
                              @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.classStructureService = classStructureService;
        this.timingLog = timingLog;
    }

    public Map<String, ChallengeDetailBundleDTO> assemble(
            UUID submissionId,
            LabRubricSnapshot rubric,
            GradingService.GradingComputationResult computed,
            Map<UUID, String> compileErrorsByChallengeId) {

        long start = System.currentTimeMillis();

        List<ChallengeRubric> challengeRubrics = rubric.byChallengeNumber().values().stream()
                .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
                .toList();
        List<UUID> challengeIds = challengeRubrics.stream()
                .map(ChallengeRubric::challengeId)
                .toList();

        SubmissionCorrectIds correctIds = correctIdsFrom(computed);
        LabChallengeStructureBundle structure = classStructureService.loadChallengeStructures(challengeIds);
        Map<UUID, String> compileErrors = compileErrorsByChallengeId != null
                ? compileErrorsByChallengeId
                : Map.of();

        Map<UUID, SubmissionTestcaseResult> testcaseResultsById = computed.testcaseResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getTestcase().getId(),
                        result -> result,
                        (left, right) -> left,
                        LinkedHashMap::new));

        Map<String, ChallengeDetailBundleDTO> labResult = new LinkedHashMap<>();
        for (ChallengeRubric challengeRubric : challengeRubrics) {
            int number = challengeRubric.challengeNumber();
            UUID challengeId = challengeRubric.challengeId();

            ChallengeSnapshot snapshot = computed.snapshotsByChallengeId != null
                    ? computed.snapshotsByChallengeId.get(challengeId)
                    : null;

            List<ClassDetailDTO> classData = classStructureService.buildClassData(
                    structure,
                    challengeId,
                    correctIds,
                    compileErrors.get(challengeId),
                    snapshot);

            MmdPillarGrader.MmdPillarResult mmdResult = computed.mmdResultsByChallengeNumber.get(number);
            ChallengeMmdMeta mmdMeta = computed.mmdMetaByChallengeId.get(challengeId);
            List<MmdClassDTO> mmdData = classStructureService.buildMmdData(
                    structure,
                    challengeId,
                    correctIds,
                    mmdResult != null ? mmdResult.outcome() : null,
                    mmdResult != null ? mmdResult.mmdSubmitted() : null,
                    mmdMeta,
                    submissionId,
                    snapshot);

            List<TestcaseResultDTO> testcases = List.of();

            PillarScoreBreakdown pillarScores = computed.pillarScoresByChallengeNumber.getOrDefault(
                    number,
                    new PillarScoreBreakdown(
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO));

            Map<String, BigDecimal> scores = Map.of(
                    "class", pillarScores.classPillar(),
                    "mmd", pillarScores.mmdPillar(),
                    "testcase", pillarScores.testcasePillar(),
                    "total", pillarScores.total());

            labResult.put("challenge_" + number, new ChallengeDetailBundleDTO(
                    classData, mmdData, testcases, scores));
        }

        if (timingLog) {
            System.out.printf("grading_timing assemble_ms=%d challenges=%d%n",
                    System.currentTimeMillis() - start, challengeRubrics.size());
        }
        return labResult;
    }

    static SubmissionCorrectIds correctIdsFrom(GradingService.GradingComputationResult computed) {
        Set<UUID> fields = new HashSet<>();
        for (SubmissionFieldResult result : computed.fieldResults) {
            if (result.isCorrect()) {
                fields.add(result.getField().getId());
            }
        }
        Set<UUID> methods = new HashSet<>();
        for (SubmissionMethodResult result : computed.methodResults) {
            if (result.isCorrect()) {
                methods.add(result.getMethod().getId());
            }
        }
        Set<UUID> constructors = new HashSet<>();
        for (SubmissionConstructorResult result : computed.constructorResults) {
            if (result.isCorrect()) {
                constructors.add(result.getConstructor().getId());
            }
        }
        Set<UUID> relations = new HashSet<>();
        for (SubmissionRelationResult result : computed.relationResults) {
            if (result.isCorrect()) {
                relations.add(result.getClassRelation().getId());
            }
        }
        return new SubmissionCorrectIds(fields, methods, constructors, relations);
    }

    private List<TestcaseResultDTO> buildTestcaseResults(
            ChallengeRubric challengeRubric,
            Map<UUID, SubmissionTestcaseResult> testcaseResultsById) {
        return List.of();
    }

    static String toFrontendResult(TestcaseResultStatus status) {
        return switch (status) {
            case PASSED -> "PASS";
            case FAILED -> "FAIL";
            case ERROR -> "ERROR";
            case SKIPPED -> "SKIPPED";
        };
    }
}
