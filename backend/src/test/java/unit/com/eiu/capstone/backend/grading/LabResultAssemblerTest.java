package unit.com.eiu.capstone.backend.grading;

import com.eiu.capstone.backend.grading.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.ChallengeDetailBundleDTO;
import com.eiu.capstone.backend.DTO.MmdClassDTO;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.testcase.TestcaseResultMapper;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.model.SubmissionTestcaseResult;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.LabChallengeStructureBundle;
import com.eiu.capstone.backend.service.SubmissionCorrectIds;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;

class LabResultAssemblerTest {

    @Test
    void assemble_doesNotReloadRubricFromDatabase() {
        UUID challengeId = UUID.randomUUID();
        ChallengeRubric challenge = challengeWithClassAndTestcase(challengeId);
        FailOnLoadClassStructureService structureService = new FailOnLoadClassStructureService();
        LabResultAssembler assembler = new LabResultAssembler(structureService, new EmptyTestcaseMapper());

        Map<String, ChallengeDetailBundleDTO> labResult = assembler.assemble(
                UUID.randomUUID(),
                new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge)),
                computed(challengeId, true, true),
                Map.of(),
                Map.of());

        ChallengeDetailBundleDTO bundle = labResult.get("challenge_1");
        assertEquals(1, bundle.getClassData().size());
        assertEquals("CakeFactory", bundle.getClassData().get(0).name());
        assertEquals(1, structureService.mmdBuilds);
    }

    @Test
    void assemble_skipsMmdAndTestcaseTreesWhenNotApplicable() {
        UUID challengeId = UUID.randomUUID();
        ChallengeRubric challenge = challengeWithClassAndTestcase(challengeId);
        FailOnLoadClassStructureService structureService = new FailOnLoadClassStructureService();
        LabResultAssembler assembler = new LabResultAssembler(structureService, new ThrowingTestcaseMapper());

        Map<String, ChallengeDetailBundleDTO> labResult = assembler.assemble(
                UUID.randomUUID(),
                new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge)),
                computed(challengeId, false, false),
                Map.of(),
                Map.of());

        ChallengeDetailBundleDTO bundle = labResult.get("challenge_1");
        assertEquals(1, bundle.getClassData().size());
        assertTrue(bundle.getMmd().classes().isEmpty());
        assertTrue(bundle.getTestcases().isEmpty());
        assertEquals(false, bundle.getScoreApplicability().get("mmd"));
        assertEquals(false, bundle.getScoreApplicability().get("testcase"));
        assertEquals(0, structureService.mmdBuilds);
    }

    @Test
    void assemble_withUnitAndCompositionRows_omitsTestcaseIoWhenPillarDark() {
        UUID challengeId = UUID.randomUUID();
        ChallengeRubric challenge = challengeWithClassAndTestcase(challengeId);
        assertEquals(2, challenge.testcases().size());
        FailOnLoadClassStructureService structureService = new FailOnLoadClassStructureService();
        LabResultAssembler assembler = new LabResultAssembler(structureService, new ThrowingTestcaseMapper());

        Map<String, ChallengeDetailBundleDTO> labResult = assembler.assemble(
                UUID.randomUUID(),
                new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge)),
                computed(challengeId, true, false),
                Map.of(),
                Map.of());

        ChallengeDetailBundleDTO bundle = labResult.get("challenge_1");
        assertEquals(1, bundle.getClassData().size());
        assertEquals("CakeFactory", bundle.getClassData().get(0).name());
        assertTrue(bundle.getTestcases().isEmpty());
        assertEquals(false, bundle.getScoreApplicability().get("testcase"));
        assertEquals(true, bundle.getScoreApplicability().get("class"));
        assertEquals(true, bundle.getScoreApplicability().get("mmd"));
        assertTrue(bundle.getScores().containsKey("class"));
        assertTrue(bundle.getScores().containsKey("mmd"));
    }

    @Test
    void assemble_defaultsTestcaseNotApplicableWhenPillarScoresMissing() {
        UUID challengeId = UUID.randomUUID();
        ChallengeRubric challenge = challengeWithClassAndTestcase(challengeId);
        FailOnLoadClassStructureService structureService = new FailOnLoadClassStructureService();
        LabResultAssembler assembler = new LabResultAssembler(structureService, new ThrowingTestcaseMapper());

        GradingService.GradingComputationResult computed = computed(challengeId, true, true);
        computed.pillarScoresByChallengeNumber = Map.of();

        Map<String, ChallengeDetailBundleDTO> labResult = assembler.assemble(
                UUID.randomUUID(),
                new LabRubricSnapshot(UUID.randomUUID(), Map.of(1, challenge)),
                computed,
                Map.of(),
                Map.of());

        ChallengeDetailBundleDTO bundle = labResult.get("challenge_1");
        assertTrue(bundle.getTestcases().isEmpty());
        assertEquals(false, bundle.getScoreApplicability().get("testcase"));
    }

    private static GradingService.GradingComputationResult computed(
            UUID challengeId,
            boolean mmdApplicable,
            boolean testcaseApplicable) {
        GradingService.GradingComputationResult computed = new GradingService.GradingComputationResult();
        computed.fieldResults = List.of();
        computed.methodResults = List.of();
        computed.constructorResults = List.of();
        computed.relationResults = List.of();
        computed.testcaseResults = List.of();
        computed.pillarScoresByChallengeNumber = Map.of(
                1,
                new PillarScoreBreakdown(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        mmdApplicable,
                        testcaseApplicable));
        computed.mmdResultsByChallengeNumber = Map.of();
        computed.mmdMetaByChallengeId = Map.of();
        computed.snapshotsByChallengeId = Map.of(challengeId, new ChallengeSnapshot());
        return computed;
    }

    private static ChallengeRubric challengeWithClassAndTestcase(UUID challengeId) {
        UUID classId = UUID.randomUUID();
        ClassRubric classRubric = new ClassRubric(
                classId,
                "CakeFactory",
                "PUBLIC",
                "CLASS",
                true,
                List.of(),
                List.of(),
                List.of());
        AssertionRubric assertion = new AssertionRubric(
                UUID.randomUUID(),
                AssertionKind.RETURN_VALUE,
                null,
                null,
                null,
                null,
                "\"1\"",
                ComparisonMode.EXACT,
                0);
        TestcaseRubric unit = new TestcaseRubric(
                UUID.randomUUID(),
                "unit-example",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                null,
                List.of(),
                List.of(assertion));
        TestcaseRubric composition = new TestcaseRubric(
                UUID.randomUUID(),
                "composition-script",
                TestcaseType.COMPOSITION,
                null,
                1,
                1,
                false,
                null,
                List.of(),
                List.of(assertion));
        return new ChallengeRubric(
                challengeId,
                1,
                "Challenge 1",
                List.of(classRubric),
                List.of(),
                List.of(unit, composition),
                false);
    }

    private static final class FailOnLoadClassStructureService extends ClassStructureService {
        private int mmdBuilds;

        private FailOnLoadClassStructureService() {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
        }

        @Override
        public LabChallengeStructureBundle loadChallengeStructures(Collection<UUID> challengeIds) {
            fail("upload assemble must not loadChallengeStructures");
            return null;
        }

        @Override
        public List<MmdClassDTO> buildMmdDataFromRubric(ChallengeRubric challengeRubric,
                                                        SubmissionCorrectIds correctIds,
                                                        com.eiu.capstone.backend.grading.MmdGradingOutcome mmdOutcome,
                                                        Boolean mmdSubmittedOverride,
                                                        ChallengeMmdMeta mmdMeta,
                                                        UUID submissionId,
                                                        ChallengeSnapshot snapshot,
                                                        com.eiu.capstone.backend.service.DisclosureMode disclosureMode) {
            mmdBuilds++;
            return super.buildMmdDataFromRubric(
                    challengeRubric,
                    correctIds,
                    mmdOutcome,
                    mmdSubmittedOverride,
                    mmdMeta,
                    submissionId,
                    snapshot,
                    disclosureMode);
        }
    }

    private static final class EmptyTestcaseMapper extends TestcaseResultMapper {
        private EmptyTestcaseMapper() {
            super(null, null);
        }

        @Override
        public List<TestcaseResultDTO> mapChallengeTestcases(
                List<TestcaseRubric> testcases,
                Map<UUID, SubmissionTestcaseResult> resultsById) {
            return List.of();
        }
    }

    private static final class ThrowingTestcaseMapper extends TestcaseResultMapper {
        private ThrowingTestcaseMapper() {
            super(null, null);
        }

        @Override
        public List<TestcaseResultDTO> mapChallengeTestcases(
                List<TestcaseRubric> testcases,
                Map<UUID, SubmissionTestcaseResult> resultsById) {
            fail("upload assemble must not map testcases when the pillar is not applicable");
            return List.of();
        }
    }
}
