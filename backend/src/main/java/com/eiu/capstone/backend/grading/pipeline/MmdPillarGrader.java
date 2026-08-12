package com.eiu.capstone.backend.grading.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.MmdParseException;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.ParsedMmdDiagram;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
import com.eiu.capstone.backend.grading.scoring.MemberWeightCalculator;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator.WeightedAccuracy;

@Component
public class MmdPillarGrader {

    private final MmdParser mmdParser;
    private final MmdComparisonService mmdComparisonService;

    public MmdPillarGrader(MmdParser mmdParser, MmdComparisonService mmdComparisonService) {
        this.mmdParser = mmdParser;
        this.mmdComparisonService = mmdComparisonService;
    }

    public MmdPillarResult grade(ChallengeRubric challengeRubric, List<MultipartFile> mmdFiles) {
        List<WeightedAccuracy> weighted = new ArrayList<>();
        List<PendingRelationResult> relations = new ArrayList<>();

        byte[] content = readFirstMmd(mmdFiles);
        boolean mmdSubmitted = content != null && content.length > 0;
        MmdGradingOutcome outcome;
        ParsedMmdDiagram diagram = null;
        if (!mmdSubmitted) {
            outcome = MmdGradingOutcome.allIncorrect(collectElements(challengeRubric));
        } else {
            try {
                diagram = mmdParser.parseBytes(content);
                outcome = mmdComparisonService.compare(challengeRubric, diagram);
            } catch (MmdParseException ex) {
                outcome = MmdGradingOutcome.allIncorrect(collectElements(challengeRubric));
            }
        }

        for (ClassRubric expectedClass : challengeRubric.classes()) {
            boolean present = outcome.isClassPresent(expectedClass.id());
            boolean typeOk = outcome.isClassCorrect(expectedClass.id());
            double classAccuracy = PartialCreditEvaluator.accuracy(List.of(present, typeOk));
            weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(), classAccuracy));

            for (var field : expectedClass.fields()) {
                boolean ok = outcome.isFieldCorrect(field.id());
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(ok)));
            }
            for (var method : expectedClass.methods()) {
                boolean ok = outcome.isMethodCorrect(method.id());
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(ok)));
            }
            for (var constructor : expectedClass.constructors()) {
                boolean ok = outcome.isConstructorCorrect(constructor.id());
                weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                        PartialCreditEvaluator.binaryAccuracy(ok)));
            }
        }

        for (RelationRubric relation : challengeRubric.relations()) {
            boolean ok = outcome.isRelationCorrect(relation.id());
            weighted.add(new WeightedAccuracy(MemberWeightCalculator.defaultMemberWeight(),
                    PartialCreditEvaluator.binaryAccuracy(ok)));
            relations.add(new PendingRelationResult(relation.id(), ok));
        }

        return new MmdPillarResult(
                PillarScoreAggregator.pillarPercentage(weighted),
                outcome,
                diagram,
                mmdSubmitted,
                relations);
    }

    private MmdGradingOutcome.ChallengeRubricElements collectElements(ChallengeRubric rubric) {
        return new MmdGradingOutcome.ChallengeRubricElements(
                rubric.classes().stream().map(ClassRubric::id).toList(),
                rubric.classes().stream().flatMap(c -> c.fields().stream()).map(f -> f.id()).toList(),
                rubric.classes().stream().flatMap(c -> c.methods().stream()).map(m -> m.id()).toList(),
                rubric.classes().stream().flatMap(c -> c.constructors().stream()).map(c -> c.id()).toList(),
                rubric.relations().stream().map(RelationRubric::id).toList());
    }

    private byte[] readFirstMmd(List<MultipartFile> mmdFiles) {
        if (mmdFiles == null || mmdFiles.isEmpty()) {
            return null;
        }
        return mmdFiles.stream()
                .sorted(java.util.Comparator.comparing(MultipartFile::getOriginalFilename, String.CASE_INSENSITIVE_ORDER))
                .findFirst()
                .map(file -> {
                    try {
                        return file.getBytes();
                    } catch (java.io.IOException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Canonical result for a challenge where {@code has_mmd} is false — the grader is never
     * invoked, so this returns a zero/empty result rather than a computed-and-discarded score.
     */
    public static MmdPillarResult notApplicable() {
        return new MmdPillarResult(BigDecimal.ZERO, new MmdGradingOutcome(), null, false, List.of());
    }

    public record MmdPillarResult(
            BigDecimal pillarPercentage,
            MmdGradingOutcome outcome,
            ParsedMmdDiagram diagram,
            boolean mmdSubmitted,
            List<PendingRelationResult> relations) {}

    public record PendingRelationResult(java.util.UUID relationId, boolean correct) {}
}
