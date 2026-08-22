package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimingLog;

@Component
public class GradingPipeline {

    private static final Pattern CHALLENGE_NUMBER_PATTERN = Pattern.compile("challenge_(\\d+)");

    private final ReflectionClassParser reflectionClassParser;
    private final ClassReflectionGrader classReflectionGrader;
    private final MmdPillarGrader mmdPillarGrader;
    private final TestcaseGrader testcaseGrader;
    private final ExecutorService pillarExecutor;
    private final boolean timingLog;

    public GradingPipeline(ReflectionClassParser reflectionClassParser,
                           ClassReflectionGrader classReflectionGrader,
                           MmdPillarGrader mmdPillarGrader,
                           TestcaseGrader testcaseGrader,
                           @Qualifier("pillarExecutor") ExecutorService pillarExecutor,
                           @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.reflectionClassParser = reflectionClassParser;
        this.classReflectionGrader = classReflectionGrader;
        this.mmdPillarGrader = mmdPillarGrader;
        this.testcaseGrader = testcaseGrader;
        this.pillarExecutor = pillarExecutor;
        this.timingLog = timingLog;
    }

    public ChallengePipelineResult gradeChallenge(
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

        Path classesDir = folderResult.folder.resolve("classes");
        long parseStart = System.currentTimeMillis();
        List<com.eiu.capstone.backend.grading.ParsedClass> parsedClasses = Files.exists(classesDir)
                ? reflectionClassParser.parseClasses(classesDir)
                : List.of();
        long parseMs = System.currentTimeMillis() - parseStart;

        ChallengeGradingContext context = ChallengeGradingContext.of(
                challengeRubric, classesDir, folderResult.compileError, parsedClasses);

        long classStart = System.currentTimeMillis();
        ClassReflectionGrader.ClassPillarResult classResult = classReflectionGrader.grade(context);
        long classMs = System.currentTimeMillis() - classStart;

        boolean mmdApplicable = challengeRubric.hasMmd();
        boolean testcaseApplicable = !challengeRubric.testcases().isEmpty();

        long[] mmdMs = {0};
        long[] testcaseMs = {0};

        CompletableFuture<MmdPillarGrader.MmdPillarResult> mmdFuture = mmdApplicable
                ? CompletableFuture.supplyAsync(() -> {
                    long started = System.currentTimeMillis();
                    MmdPillarGrader.MmdPillarResult result = mmdPillarGrader.grade(challengeRubric, mmdFiles);
                    mmdMs[0] = System.currentTimeMillis() - started;
                    return result;
                }, pillarExecutor)
                : CompletableFuture.completedFuture(MmdPillarGrader.notApplicable());
        CompletableFuture<TestcaseGrader.TestcasePillarResult> testcaseFuture = testcaseApplicable
                ? CompletableFuture.supplyAsync(() -> {
                    long started = System.currentTimeMillis();
                    TestcaseGrader.TestcasePillarResult result = testcaseGrader.grade(context);
                    testcaseMs[0] = System.currentTimeMillis() - started;
                    return result;
                }, pillarExecutor)
                : CompletableFuture.completedFuture(TestcaseGrader.TestcasePillarResult.empty());

        CompletableFuture.allOf(mmdFuture, testcaseFuture).join();
        MmdPillarGrader.MmdPillarResult mmdResult = mmdFuture.join();
        TestcaseGrader.TestcasePillarResult testcaseResult = testcaseFuture.join();

        long scoreStart = System.currentTimeMillis();
        BigDecimal challengePct = PillarScoreAggregator.challengePercentage(
                classResult.pillarPercentage(),
                challengeRubric.classWeight(),
                mmdResult.pillarPercentage(),
                mmdApplicable,
                challengeRubric.mmdWeight(),
                testcaseResult.pillarPercentage(),
                testcaseApplicable,
                challengeRubric.testcaseWeight());
        long scoreMs = System.currentTimeMillis() - scoreStart;
        TimingLog.block(timingLog, "Challenge " + challengeKey,
                "parse", parseMs,
                "class", classMs,
                "mmd", mmdMs[0],
                "testcase", testcaseMs[0],
                "score", scoreMs,
                "total", System.currentTimeMillis() - challengeStart);

        boolean fullyCorrect = classResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0
                && (!mmdApplicable || mmdResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0)
                && (!testcaseApplicable || testcaseResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0);

        return new ChallengePipelineResult(
                challengeNumber,
                challengeRubric.challengeId(),
                challengeRubric.name(),
                challengePct,
                fullyCorrect,
                mmdApplicable,
                testcaseApplicable,
                classResult,
                mmdResult,
                testcaseResult,
                context);
    }

    private Integer extractChallengeNumber(String challengeFolderKey) {
        Matcher m = CHALLENGE_NUMBER_PATTERN.matcher(challengeFolderKey);
        return m.matches() ? Integer.parseInt(m.group(1)) : null;
    }

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
