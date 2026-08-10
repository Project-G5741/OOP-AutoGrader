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
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.ReflectionClassParser;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.service.SubmissionStorageService;

@Component
public class GradingPipeline {

    private static final Pattern CHALLENGE_NUMBER_PATTERN = Pattern.compile("challenge_(\\d+)");

    private final ReflectionClassParser reflectionClassParser;
    private final ClassReflectionGrader classReflectionGrader;
    private final MmdPillarGrader mmdPillarGrader;
    private final TestcaseGrader testcaseGrader;
    private final ExecutorService gradingExecutor;

    public GradingPipeline(ReflectionClassParser reflectionClassParser,
                           ClassReflectionGrader classReflectionGrader,
                           MmdPillarGrader mmdPillarGrader,
                           TestcaseGrader testcaseGrader,
                           @Qualifier("gradingExecutor") ExecutorService gradingExecutor) {
        this.reflectionClassParser = reflectionClassParser;
        this.classReflectionGrader = classReflectionGrader;
        this.mmdPillarGrader = mmdPillarGrader;
        this.testcaseGrader = testcaseGrader;
        this.gradingExecutor = gradingExecutor;
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

        Path classesDir = folderResult.folder.resolve("classes");
        List<com.eiu.capstone.backend.grading.ParsedClass> parsedClasses = Files.exists(classesDir)
                ? reflectionClassParser.parseClasses(classesDir)
                : List.of();
        ChallengeGradingContext context = ChallengeGradingContext.of(
                challengeRubric, classesDir, folderResult.compileError, parsedClasses);

        ClassReflectionGrader.ClassPillarResult classResult = classReflectionGrader.grade(context);

        CompletableFuture<MmdPillarGrader.MmdPillarResult> mmdFuture = CompletableFuture.supplyAsync(
                () -> mmdPillarGrader.grade(challengeRubric, mmdFiles), gradingExecutor);
        CompletableFuture<TestcaseGrader.TestcasePillarResult> testcaseFuture = CompletableFuture.supplyAsync(
                () -> testcaseGrader.grade(context), gradingExecutor);

        CompletableFuture.allOf(mmdFuture, testcaseFuture).join();
        MmdPillarGrader.MmdPillarResult mmdResult = mmdFuture.join();
        TestcaseGrader.TestcasePillarResult testcaseResult = testcaseFuture.join();

        BigDecimal challengePct = PillarScoreAggregator.challengePercentage(
                classResult.pillarPercentage(),
                mmdResult.pillarPercentage(),
                testcaseResult.pillarPercentage());

        boolean fullyCorrect = classResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0
                && mmdResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0
                && testcaseResult.pillarPercentage().compareTo(BigDecimal.valueOf(100)) == 0;

        return new ChallengePipelineResult(
                challengeNumber,
                challengeRubric.challengeId(),
                challengeRubric.name(),
                challengePct,
                fullyCorrect,
                classResult,
                mmdResult,
                testcaseResult,
                parsedClasses);
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
            ClassReflectionGrader.ClassPillarResult classResult,
            MmdPillarGrader.MmdPillarResult mmdResult,
            TestcaseGrader.TestcasePillarResult testcaseResult,
            List<com.eiu.capstone.backend.grading.ParsedClass> parsedClasses) {}
}
