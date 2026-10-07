package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.testcase.WorkerSessionHandle;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimingLog;

@Component
public class GradingPipeline {

    private static final Pattern CHALLENGE_NUMBER_PATTERN = Pattern.compile("challenge_(\\d+)");

    private final ReflectionClassParser reflectionClassParser;
    private final ClassReflectionGrader classReflectionGrader;
    private final MmdPillarGrader mmdPillarGrader;
    private final TestcaseGrader testcaseGrader;
    private final boolean timingLog;

    public GradingPipeline(ReflectionClassParser reflectionClassParser,
                           ClassReflectionGrader classReflectionGrader,
                           MmdPillarGrader mmdPillarGrader,
                           TestcaseGrader testcaseGrader,
                           @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.reflectionClassParser = reflectionClassParser;
        this.classReflectionGrader = classReflectionGrader;
        this.mmdPillarGrader = mmdPillarGrader;
        this.testcaseGrader = testcaseGrader;
        this.timingLog = timingLog;
    }

    /** Grades one challenge without a shared worker session (operational testcases deferred). */
    public ChallengePipelineResult gradeChallenge(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult,
            List<MultipartFile> mmdFiles) {
        return gradeChallenge(rubric, folderResult, mmdFiles, null);
    }

    public ChallengePipelineResult gradeChallenge(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult,
            List<MultipartFile> mmdFiles,
            WorkerSessionHandle workerSession) {

        ClassMmdPhaseResult phase = gradeClassAndMmd(rubric, folderResult, mmdFiles);
        if (phase == null) {
            return null;
        }
        if (!phase.testcaseApplicable() || workerSession == null) {
            return toPipelineResult(phase, TestcaseGrader.TestcasePillarResult.empty(), phase.context());
        }
        return completeOperationalTestcases(phase, workerSession);
    }

    /**
     * Class reflection, then MMD when applicable. Operational testcases are intentionally omitted
     * so upload grading can finish this phase before opening the isolated worker JVM.
     */
    public ClassMmdPhaseResult gradeClassAndMmd(
            LabRubricSnapshot rubric,
            SubmissionStorageService.ChallengeResult folderResult,
            List<MultipartFile> mmdFiles) {

        Integer challengeNumber = extractChallengeNumber(folderResult.challengeName);
        if (challengeNumber == null) {
            return null;
        }
        ChallengeRubric challengeRubric = rubric.challenge(challengeNumber).orElse(null);
        if (challengeRubric == null) {
            return null;
        }

        long challengeStart = System.currentTimeMillis();
        String challengeKey = folderResult.challengeName;

        boolean mmdApplicable = challengeRubric.hasMmd();
        boolean testcaseApplicable = !challengeRubric.testcases().isEmpty();

        Path classesDir = folderResult.folder.resolve("classes");
        long parseStart = System.currentTimeMillis();
        List<com.eiu.capstone.backend.grading.ParsedClass> parsedClasses = Files.exists(classesDir)
                ? reflectionClassParser.parseClasses(classesDir)
                : List.of();
        long parseMs = System.currentTimeMillis() - parseStart;

        ChallengeGradingContext context = ChallengeGradingContext.of(
                challengeRubric,
                classesDir,
                folderResult.compileError,
                parsedClasses,
                folderResult.failedClassNames,
                folderResult.compileErrorsByClassName,
                null);

        long classStart = System.currentTimeMillis();
        ClassReflectionGrader.ClassPillarResult classResult = classReflectionGrader.grade(context);
        long classMs = System.currentTimeMillis() - classStart;

        long mmdStart = System.currentTimeMillis();
        MmdPillarGrader.MmdPillarResult mmdResult = mmdApplicable
                ? mmdPillarGrader.grade(challengeRubric, mmdFiles)
                : MmdPillarGrader.notApplicable();
        long mmdMs = System.currentTimeMillis() - mmdStart;

        TimingLog.block(timingLog, "Challenge " + challengeKey + " (class+mmd)",
                "parse", parseMs,
                "class", classMs,
                "mmd", mmdMs,
                "total", System.currentTimeMillis() - challengeStart);

        return new ClassMmdPhaseResult(
                challengeNumber,
                challengeRubric.challengeId(),
                challengeRubric.name(),
                challengeRubric,
                mmdApplicable,
                testcaseApplicable,
                classResult,
                mmdResult,
                context);
    }

    /** Runs operational testcases after class and MMD pillars have finished for this challenge. */
    public ChallengePipelineResult completeOperationalTestcases(
            ClassMmdPhaseResult phase,
            WorkerSessionHandle workerSession) {

        long challengeStart = System.currentTimeMillis();
        long testcaseStart = System.currentTimeMillis();
        ChallengeGradingContext context = phase.context().withWorkerSession(workerSession);
        TestcaseGrader.TestcasePillarResult testcaseResult = testcaseGrader.grade(context);
        long testcaseMs = System.currentTimeMillis() - testcaseStart;

        TimingLog.block(timingLog, "Challenge " + phase.challengeNumber() + " (testcase)",
                "testcase", testcaseMs,
                "total", System.currentTimeMillis() - challengeStart);

        return toPipelineResult(phase, testcaseResult, context);
    }

    /**
     * Lab-wide OT: one worker batch across all OT-applicable challenges, then per-challenge finish.
     * Non-OT phases keep empty testcase pillars. Result list aligns with {@code phases}.
     */
    public List<ChallengePipelineResult> completeOperationalTestcasesLabWide(
            List<ClassMmdPhaseResult> phases,
            WorkerSessionHandle workerSession,
            int batchParallelism) {

        if (phases == null || phases.isEmpty()) {
            return List.of();
        }
        long started = System.currentTimeMillis();
        List<ChallengeGradingContext> otContexts = new ArrayList<>();
        List<Integer> otIndexes = new ArrayList<>();
        for (int i = 0; i < phases.size(); i++) {
            ClassMmdPhaseResult phase = phases.get(i);
            if (phase != null && phase.testcaseApplicable()) {
                otIndexes.add(i);
                otContexts.add(phase.context().withWorkerSession(workerSession));
            }
        }
        List<TestcaseGrader.TestcasePillarResult> otResults = otContexts.isEmpty()
                ? List.of()
                : testcaseGrader.gradeLab(otContexts, workerSession, batchParallelism);
        TimingLog.block(timingLog, "Lab OT (lab-wide batch)",
                "challenges", otContexts.size(),
                "testcase", System.currentTimeMillis() - started,
                "total", System.currentTimeMillis() - started);

        List<ChallengePipelineResult> out = new ArrayList<>(phases.size());
        int otCursor = 0;
        for (int i = 0; i < phases.size(); i++) {
            ClassMmdPhaseResult phase = phases.get(i);
            if (phase == null) {
                out.add(null);
                continue;
            }
            if (!phase.testcaseApplicable()) {
                out.add(toPipelineResult(phase, TestcaseGrader.TestcasePillarResult.empty(), phase.context()));
                continue;
            }
            TestcaseGrader.TestcasePillarResult testcaseResult = otCursor < otResults.size()
                    ? otResults.get(otCursor++)
                    : TestcaseGrader.TestcasePillarResult.empty();
            ChallengeGradingContext context = phase.context().withWorkerSession(workerSession);
            out.add(toPipelineResult(phase, testcaseResult, context));
        }
        return out;
    }

    private ChallengePipelineResult toPipelineResult(
            ClassMmdPhaseResult phase,
            TestcaseGrader.TestcasePillarResult testcaseResult,
            ChallengeGradingContext gradingContext) {

        ChallengeRubric challengeRubric = phase.challengeRubric();
        BigDecimal challengePct = PillarScoreAggregator.challengePercentage(
                phase.classResult().pillarPercentage(),
                challengeRubric.classWeight(),
                phase.mmdResult().pillarPercentage(),
                phase.mmdApplicable(),
                challengeRubric.mmdWeight(),
                testcaseResult.pillarPercentage(),
                phase.testcaseApplicable(),
                challengeRubric.testcaseWeight());

        boolean fullyCorrect = phase.classResult().pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0
                && (!phase.mmdApplicable()
                || phase.mmdResult().pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0)
                && (!phase.testcaseApplicable()
                || testcaseResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0);

        return new ChallengePipelineResult(
                phase.challengeNumber(),
                phase.challengeId(),
                phase.challengeName(),
                challengePct,
                fullyCorrect,
                phase.mmdApplicable(),
                phase.testcaseApplicable(),
                phase.classResult(),
                phase.mmdResult(),
                testcaseResult,
                gradingContext);
    }

    private Integer extractChallengeNumber(String challengeFolderKey) {
        Matcher m = CHALLENGE_NUMBER_PATTERN.matcher(challengeFolderKey);
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

    public record ClassMmdPhaseResult(
            int challengeNumber,
            java.util.UUID challengeId,
            String challengeName,
            ChallengeRubric challengeRubric,
            boolean mmdApplicable,
            boolean testcaseApplicable,
            ClassReflectionGrader.ClassPillarResult classResult,
            MmdPillarGrader.MmdPillarResult mmdResult,
            ChallengeGradingContext context) {}

    public record ChallengePipelineResult(
            int challengeNumber,
            java.util.UUID challengeId,
            String challengeName,
            BigDecimal percentage,
            boolean fullyCorrect,
            boolean mmdApplicable,
            boolean testcaseApplicable,
            ClassReflectionGrader.ClassPillarResult classResult,
            MmdPillarGrader.MmdPillarResult mmdResult,
            TestcaseGrader.TestcasePillarResult testcaseResult,
            ChallengeGradingContext gradingContext) {}
}
