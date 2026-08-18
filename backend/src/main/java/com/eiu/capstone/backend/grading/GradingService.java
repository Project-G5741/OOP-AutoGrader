package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.pipeline.GradingPipeline;
import com.eiu.capstone.backend.grading.pipeline.GradingPipeline.ChallengePipelineResult;
import com.eiu.capstone.backend.grading.pipeline.MmdPillarGrader;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.SubmissionMethodResult;
import com.eiu.capstone.backend.model.SubmissionRelationResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseAssertionResult;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.service.ParsedSubmissionSnapshotStore;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshotBuilder;
import com.eiu.capstone.backend.utility.CompletableFutures;

@Service
public class GradingService {

    private static final Pattern CHALLENGE_NUMBER_PATTERN =
            Pattern.compile("challenge_(\\d+)", Pattern.CASE_INSENSITIVE);

    private final ChallengeRepository challengeRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final MmdComparisonService mmdComparisonService;
    private final ExecutorService gradingExecutor;
    private final GradingResultStore gradingResultStore;
    private final ClassRelationRepository classRelationRepository;
    private final GradingPipeline gradingPipeline;
    private final TestcaseRepository testcaseRepository;
    private final TestcaseAssertionRepository testcaseAssertionRepository;
    private final LabResultAssembler labResultAssembler;
    private final ParsedSubmissionSnapshotBuilder parsedSubmissionSnapshotBuilder;
    private final ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;

    public GradingService(ChallengeRepository challengeRepository,
                          FieldRepository fieldRepository,
                          MethodRepository methodRepository,
                          ConstructorRepository constructorRepository,
                          MmdComparisonService mmdComparisonService,
                          @Qualifier("gradingExecutor") ExecutorService gradingExecutor,
                          GradingResultStore gradingResultStore,
                          ClassRelationRepository classRelationRepository,
                          GradingPipeline gradingPipeline,
                          TestcaseRepository testcaseRepository,
                          TestcaseAssertionRepository testcaseAssertionRepository,
                          LabResultAssembler labResultAssembler,
                          ParsedSubmissionSnapshotBuilder parsedSubmissionSnapshotBuilder,
                          ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore) {
        this.challengeRepository = challengeRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.mmdComparisonService = mmdComparisonService;
        this.gradingExecutor = gradingExecutor;
        this.gradingResultStore = gradingResultStore;
        this.classRelationRepository = classRelationRepository;
        this.gradingPipeline = gradingPipeline;
        this.testcaseRepository = testcaseRepository;
        this.testcaseAssertionRepository = testcaseAssertionRepository;
        this.labResultAssembler = labResultAssembler;
        this.parsedSubmissionSnapshotBuilder = parsedSubmissionSnapshotBuilder;
        this.parsedSubmissionSnapshotStore = parsedSubmissionSnapshotStore;
    }

    public GradingOutcome gradeSubmission(LabSubmission submission,
                                      LabRubricSnapshot rubric,
                                      List<SubmissionStorageService.ChallengeResult> challengeFolderResults,
                                      Map<String, List<MultipartFile>> mmdByChallenge,
                                      boolean skipExistingLoad) {

        GradingService.ExistingResults existing = skipExistingLoad
                ? emptyExistingResults()
                : gradingResultStore.loadExisting(submission);
        GradingComputationResult computed = computeAgainstSnapshot(
                rubric, challengeFolderResults, mmdByChallenge, submission, existing);
        gradingResultStore.save(computed);
        parsedSubmissionSnapshotStore.save(submission.getId(), computed.snapshotsByChallengeId);

        var labResult = labResultAssembler.assemble(
                submission.getId(),
                rubric,
                computed,
                compileErrorsByChallengeId(rubric, challengeFolderResults),
                packageNormalizationNoticesByChallengeId(rubric, challengeFolderResults));
        return new GradingOutcome(
                computed.overallScore,
                computed.gradedChallenges,
                computed.mmdMetaByChallengeId,
                labResult);
    }

    private static ExistingResults emptyExistingResults() {
        ExistingResults existing = new ExistingResults();
        existing.fieldResults = Map.of();
        existing.methodResults = Map.of();
        existing.constructorResults = Map.of();
        existing.relationResults = Map.of();
        existing.challengeResults = Map.of();
        existing.testcaseResults = Map.of();
        return existing;
    }

    private GradingComputationResult computeAgainstSnapshot(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challengeFolderResults,
            Map<String, List<MultipartFile>> mmdByChallenge,
            LabSubmission submission,
            ExistingResults existing) {

        List<CompletableFuture<ChallengeComputation>> futures = challengeFolderResults.stream()
                .map(folderResult -> CompletableFuture.supplyAsync(
                        () -> gradeChallengeFolder(rubric, folderResult,
                                mmdByChallenge.getOrDefault(folderResult.challengeName, List.of())),
                        gradingExecutor))
                .collect(Collectors.toList());

        List<ChallengeComputation> challengeComputations = CompletableFutures.joinAll(futures);

        GradingComputationResult result = new GradingComputationResult();
        result.fieldResults = new ArrayList<>();
        result.methodResults = new ArrayList<>();
        result.constructorResults = new ArrayList<>();
        result.relationResults = new ArrayList<>();
        result.challengeResults = new ArrayList<>();
        result.testcaseResults = new ArrayList<>();
        result.challengePercentages = new ArrayList<>();
        result.gradedChallenges = new ArrayList<>();
        result.mmdMetaByChallengeId = new java.util.LinkedHashMap<>();
        result.pillarScoresByChallengeNumber = new java.util.LinkedHashMap<>();
        result.mmdResultsByChallengeNumber = new java.util.LinkedHashMap<>();
        result.snapshotsByChallengeId = new java.util.LinkedHashMap<>();

        Map<Integer, BigDecimal> percentagesByChallengeNumber = new java.util.LinkedHashMap<>();
        for (ChallengeComputation cc : challengeComputations) {
            if (cc == null) continue;
            if (cc.challengeId != null && cc.mmdMeta != null) {
                result.mmdMetaByChallengeId.put(cc.challengeId, cc.mmdMeta);
            }
            for (PendingFieldResult pending : cc.pendingFields) {
                result.fieldResults.add(buildFieldResult(existing.fieldResults, submission, pending.fieldId(), pending.correct()));
            }
            for (PendingMethodResult pending : cc.pendingMethods) {
                result.methodResults.add(buildMethodResult(existing.methodResults, submission, pending.methodId(), pending.correct()));
            }
            for (PendingConstructorResult pending : cc.pendingConstructors) {
                result.constructorResults.add(buildConstructorResult(
                        existing.constructorResults, submission, pending.constructorId(), pending.correct()));
            }
            for (PendingRelationResult pending : cc.pendingRelations) {
                result.relationResults.add(buildRelationResult(
                        existing.relationResults, submission, pending.relationId(), pending.correct()));
            }
            for (PendingTestcaseResult pending : cc.pendingTestcases) {
                result.testcaseResults.add(buildTestcaseResult(
                        existing.testcaseResults, submission, pending));
            }
            if (cc.pendingChallenge != null) {
                BigDecimal challengeScore = cc.percentage != null ? cc.percentage : BigDecimal.ZERO;
                result.challengeResults.add(buildChallengeResult(
                        existing.challengeResults,
                        submission,
                        cc.pendingChallenge.challengeId(),
                        cc.pendingChallenge.correct(),
                        challengeScore));
            }
            if (cc.challengeNumber != null && cc.percentage != null) {
                percentagesByChallengeNumber.put(cc.challengeNumber, cc.percentage);
            }
            if (cc.challengeNumber != null) {
                result.pillarScoresByChallengeNumber.put(
                        cc.challengeNumber,
                        new PillarScoreBreakdown(
                                cc.classPillarPct != null ? cc.classPillarPct : BigDecimal.ZERO,
                                cc.mmdPillarPct != null ? cc.mmdPillarPct : BigDecimal.ZERO,
                                cc.testcasePillarPct != null ? cc.testcasePillarPct : BigDecimal.ZERO,
                                cc.percentage != null ? cc.percentage : BigDecimal.ZERO,
                                cc.mmdApplicable,
                                cc.testcaseApplicable));
            }
            if (cc.challengeNumber != null && cc.mmdResult != null) {
                result.mmdResultsByChallengeNumber.put(cc.challengeNumber, cc.mmdResult);
            }
            if (cc.challengeId != null && cc.snapshot != null) {
                result.snapshotsByChallengeId.put(cc.challengeId, cc.snapshot);
            }
            if (cc.challengeId != null && cc.percentage != null) {
                result.gradedChallenges.add(new GradedChallengeSummary(
                        cc.challengeId,
                        cc.percentage.setScale(0, RoundingMode.HALF_UP).intValue()));
            }
        }

        List<BigDecimal> overallChallengeScores = new ArrayList<>();
        for (ChallengeRubric challengeRubric : rubric.byChallengeNumber().values().stream()
                .sorted(Comparator.comparingInt(ChallengeRubric::challengeNumber))
                .toList()) {
            BigDecimal challengeScore = percentagesByChallengeNumber.getOrDefault(challengeRubric.challengeNumber(), BigDecimal.ZERO);
            result.challengePercentages.add(challengeScore);
            overallChallengeScores.add(challengeScore);
        }

        result.overallScore = PillarScoreAggregator.labPercentage(overallChallengeScores);
        return result;
    }

    private ChallengeComputation gradeChallengeFolder(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult,
            List<MultipartFile> mmdFiles) {

        ChallengePipelineResult pipelineResult = gradingPipeline.gradeChallenge(rubric, folderResult, mmdFiles);
        if (pipelineResult == null) {
            return null;
        }

        ChallengeRubric challengeRubric = rubric.challenge(pipelineResult.challengeNumber()).orElse(null);
        if (challengeRubric == null) {
            return null;
        }

        ChallengeComputation computation = new ChallengeComputation();
        computation.challengeNumber = pipelineResult.challengeNumber();
        computation.challengeId = pipelineResult.challengeId();
        computation.percentage = pipelineResult.percentage();
        computation.pendingFields = pipelineResult.classResult().fields().stream()
                .map(f -> new PendingFieldResult(f.fieldId(), f.correct()))
                .toList();
        computation.pendingMethods = pipelineResult.classResult().methods().stream()
                .map(m -> new PendingMethodResult(m.methodId(), m.correct()))
                .toList();
        computation.pendingConstructors = pipelineResult.classResult().constructors().stream()
                .map(c -> new PendingConstructorResult(c.constructorId(), c.correct()))
                .toList();
        computation.pendingRelations = pipelineResult.mmdResult().relations().stream()
                .map(r -> new PendingRelationResult(r.relationId(), r.correct()))
                .toList();
        computation.pendingTestcases = pipelineResult.testcaseResult().results().stream()
                .map(t -> new PendingTestcaseResult(
                        t.testcaseId(),
                        t.status(),
                        t.feedback(),
                        t.inputDisplay(),
                        t.expectedDisplay(),
                        t.actualDisplay(),
                        t.assertions()))
                .toList();
        computation.pendingChallenge = new PendingChallengeResult(
                pipelineResult.challengeId(), pipelineResult.fullyCorrect());
        computation.mmdMeta = buildMmdMeta(challengeRubric, pipelineResult.mmdResult());
        computation.mmdResult = pipelineResult.mmdResult();
        computation.classPillarPct = pipelineResult.classResult().pillarPercentage();
        computation.mmdPillarPct = pipelineResult.mmdResult().pillarPercentage();
        computation.testcasePillarPct = pipelineResult.testcaseResult().pillarPercentage();
        computation.mmdApplicable = pipelineResult.mmdApplicable();
        computation.testcaseApplicable = pipelineResult.testcaseApplicable();
        computation.snapshot = parsedSubmissionSnapshotBuilder.build(
                challengeRubric,
                pipelineResult.parsedClasses(),
                pipelineResult.mmdResult().diagram());
        return computation;
    }

    private com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta buildMmdMeta(
            ChallengeRubric challengeRubric,
            com.eiu.capstone.backend.grading.pipeline.MmdPillarGrader.MmdPillarResult mmdResult) {
        com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta meta =
                new com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta();
        meta.mmdSubmitted = mmdResult.mmdSubmitted();
        Map<String, Boolean> stereotypeMap = new java.util.HashMap<>();
        for (ClassRubric expectedClass : challengeRubric.classes()) {
            stereotypeMap.put(expectedClass.id().toString(), mmdResult.outcome().isClassPresent(expectedClass.id()));
        }
        meta.classStereotypeCorrect = stereotypeMap;

        Map<String, String> relationErrors = new java.util.HashMap<>();
        for (RelationRubric expectedRelation : challengeRubric.relations()) {
            if (mmdResult.outcome().isRelationCorrect(expectedRelation.id())) {
                continue;
            }
            relationErrors.put(expectedRelation.id().toString(),
                    mmdResult.diagram() != null && mmdComparisonService.relationPresentInDiagram(
                            expectedRelation, mmdResult.diagram())
                            ? "Relation mismatch" : "Missing relationship");
        }
        meta.relationErrors = relationErrors;
        return meta;
    }

    private Map<UUID, String> compileErrorsByChallengeId(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challengeFolderResults) {
        Map<UUID, String> errors = new LinkedHashMap<>();
        if (challengeFolderResults == null) {
            return errors;
        }
        for (SubmissionStorageService.ChallengeResult folder : challengeFolderResults) {
            if (folder.compileError == null || folder.compileError.isBlank()) {
                continue;
            }
            Matcher matcher = CHALLENGE_NUMBER_PATTERN.matcher(folder.challengeName);
            if (!matcher.matches()) {
                continue;
            }
            int challengeNumber = Integer.parseInt(matcher.group(1));
            rubric.challenge(challengeNumber).ifPresent(challengeRubric ->
                    errors.put(challengeRubric.challengeId(), folder.compileError));
        }
        return errors;
    }

    private Map<UUID, String> packageNormalizationNoticesByChallengeId(
            LabRubricSnapshot rubric,
            List<SubmissionStorageService.ChallengeResult> challengeFolderResults) {
        Map<UUID, String> notices = new LinkedHashMap<>();
        if (challengeFolderResults == null) {
            return notices;
        }
        for (SubmissionStorageService.ChallengeResult folder : challengeFolderResults) {
            if (folder.packageNormalizationNotice == null || folder.packageNormalizationNotice.isBlank()) {
                continue;
            }
            Matcher matcher = CHALLENGE_NUMBER_PATTERN.matcher(folder.challengeName);
            if (!matcher.matches()) {
                continue;
            }
            int challengeNumber = Integer.parseInt(matcher.group(1));
            rubric.challenge(challengeNumber).ifPresent(challengeRubric ->
                    notices.put(challengeRubric.challengeId(), folder.packageNormalizationNotice));
        }
        return notices;
    }

    private SubmissionTestcaseResult buildTestcaseResult(Map<UUID, SubmissionTestcaseResult> existing,
                                                         LabSubmission submission,
                                                         PendingTestcaseResult pending) {
        SubmissionTestcaseResult result = existing.getOrDefault(pending.testcaseId(), new SubmissionTestcaseResult());
        result.setSubmission(submission);
        result.setTestcase(testcaseRepository.getReferenceById(pending.testcaseId()));
        result.setResult(pending.status());
        result.setFeedback(pending.feedback());
        result.setInputDisplay(pending.inputDisplay());
        result.setExpectedDisplay(pending.expectedDisplay());
        result.setActualDisplay(pending.actualDisplay());

        Map<UUID, SubmissionTestcaseAssertionResult> existingAssertions = result.getAssertionResults().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.getTestcaseAssertion().getId(),
                        row -> row,
                        (left, right) -> left));

        result.getAssertionResults().clear();
        for (com.eiu.capstone.backend.grading.pipeline.TestcaseGrader.PendingAssertionResult assertionPending
                : pending.assertions()) {
            SubmissionTestcaseAssertionResult assertionResult = existingAssertions.getOrDefault(
                    assertionPending.assertionId(), new SubmissionTestcaseAssertionResult());
            assertionResult.setSubmissionTestcaseResult(result);
            assertionResult.setTestcaseAssertion(
                    testcaseAssertionRepository.getReferenceById(assertionPending.assertionId()));
            assertionResult.setResult(assertionPending.status());
            assertionResult.setActualValue(assertionPending.actualValueJson());
            assertionResult.setFeedback(assertionPending.feedback());
            result.getAssertionResults().add(assertionResult);
        }
        return result;
    }

    private SubmissionFieldResult buildFieldResult(Map<UUID, SubmissionFieldResult> existing,
                                                   LabSubmission submission,
                                                   UUID fieldId,
                                                   boolean correct) {
        SubmissionFieldResult result = existing.getOrDefault(fieldId, new SubmissionFieldResult());
        result.setSubmission(submission);
        result.setField(fieldRepository.getReferenceById(fieldId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionMethodResult buildMethodResult(Map<UUID, SubmissionMethodResult> existing,
                                                     LabSubmission submission,
                                                     UUID methodId,
                                                     boolean correct) {
        SubmissionMethodResult result = existing.getOrDefault(methodId, new SubmissionMethodResult());
        result.setSubmission(submission);
        result.setMethod(methodRepository.getReferenceById(methodId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionConstructorResult buildConstructorResult(Map<UUID, SubmissionConstructorResult> existing,
                                                               LabSubmission submission,
                                                               UUID constructorId,
                                                               boolean correct) {
        SubmissionConstructorResult result = existing.getOrDefault(constructorId, new SubmissionConstructorResult());
        result.setSubmission(submission);
        result.setConstructor(constructorRepository.getReferenceById(constructorId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionRelationResult buildRelationResult(Map<UUID, SubmissionRelationResult> existing,
                                                         LabSubmission submission,
                                                         UUID relationId,
                                                         boolean correct) {
        SubmissionRelationResult result = existing.getOrDefault(relationId, new SubmissionRelationResult());
        result.setSubmission(submission);
        result.setClassRelation(classRelationRepository.getReferenceById(relationId));
        result.setCorrect(correct);
        return result;
    }

    private SubmissionChallengeResult buildChallengeResult(Map<UUID, SubmissionChallengeResult> existing,
                                                           LabSubmission submission,
                                                           UUID challengeId,
                                                           boolean correct,
                                                           BigDecimal score) {
        SubmissionChallengeResult result = existing.getOrDefault(challengeId, new SubmissionChallengeResult());
        result.setSubmission(submission);
        result.setChallenge(challengeRepository.getReferenceById(challengeId));
        result.setCorrect(correct);
        result.setScore(score);
        return result;
    }

    static class ExistingResults {
        Map<UUID, SubmissionFieldResult> fieldResults;
        Map<UUID, SubmissionMethodResult> methodResults;
        Map<UUID, SubmissionConstructorResult> constructorResults;
        Map<UUID, SubmissionRelationResult> relationResults;
        Map<UUID, SubmissionChallengeResult> challengeResults;
        Map<UUID, SubmissionTestcaseResult> testcaseResults;
    }

    static class GradingComputationResult {
        List<SubmissionFieldResult> fieldResults;
        List<SubmissionMethodResult> methodResults;
        List<SubmissionConstructorResult> constructorResults;
        List<SubmissionRelationResult> relationResults;
        List<SubmissionChallengeResult> challengeResults;
        List<SubmissionTestcaseResult> testcaseResults;
        List<BigDecimal> challengePercentages;
        List<GradedChallengeSummary> gradedChallenges;
        BigDecimal overallScore;
        Map<UUID, com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta> mmdMetaByChallengeId;
        Map<Integer, PillarScoreBreakdown> pillarScoresByChallengeNumber;
        Map<Integer, MmdPillarGrader.MmdPillarResult> mmdResultsByChallengeNumber;
        Map<UUID, ChallengeSnapshot> snapshotsByChallengeId;
    }

    private static class ChallengeComputation {
        Integer challengeNumber;
        UUID challengeId;
        BigDecimal percentage;
        BigDecimal classPillarPct;
        BigDecimal mmdPillarPct;
        BigDecimal testcasePillarPct;
        boolean mmdApplicable;
        boolean testcaseApplicable;
        MmdPillarGrader.MmdPillarResult mmdResult;
        PendingChallengeResult pendingChallenge;
        List<PendingFieldResult> pendingFields;
        List<PendingMethodResult> pendingMethods;
        List<PendingConstructorResult> pendingConstructors;
        List<PendingRelationResult> pendingRelations;
        List<PendingTestcaseResult> pendingTestcases;
        com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta mmdMeta;
        ChallengeSnapshot snapshot;
    }
}
