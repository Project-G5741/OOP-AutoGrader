package unit.com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.ChallengeSource;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.Recovered;
import com.eiu.capstone.backend.analytics.service.AttemptPillarRecovery.Sources;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.MmdOutcomesCodec;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;

class AttemptPillarRecoveryTest {

    @Test
    void declarationOnlyChallengeUsesStoredScore() {
        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        LabRubricSnapshot rubric = rubric(challenge(challengeId, classId, methodId, false));
        Recovered recovered = AttemptPillarRecovery.recover(rubric, new Sources(
                List.of(new ChallengeSource(challengeId, new BigDecimal("80.00"), null)),
                Map.of(), Map.of(), Map.of(), Map.of()));

        assertNull(recovered.lab().mmdScore());
        assertNull(recovered.lab().operationalScore());
        assertEquals(new BigDecimal("80.00"), recovered.lab().declarationScore());
    }

    @Test
    void solvesDeclarationFromMmdAndChallengeScore() {
        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        MmdGradingOutcome outcome = new MmdGradingOutcome();
        outcome.setClassPresent(classId, true);
        outcome.setClass(classId, true);
        ClassRubric classRubric = new ClassRubric(
                classId, "Box", "public", "class", false, List.of(), List.of(), List.of(), 1);
        ChallengeRubric challenge = new ChallengeRubric(
                challengeId, 1, "Challenge 1", List.of(classRubric), List.of(), List.of(),
                true, true, 1, 1, 1, 1);
        LabRubricSnapshot rubric = rubric(challenge);
        Recovered recovered = AttemptPillarRecovery.recover(rubric, new Sources(
                List.of(new ChallengeSource(challengeId, new BigDecimal("75.00"), MmdOutcomesCodec.toJson(outcome))),
                Map.of(), Map.of(), Map.of(), Map.of()));

        assertEquals(new BigDecimal("100.00"), recovered.lab().mmdScore());
        assertEquals(new BigDecimal("50.00"), recovered.lab().declarationScore());
        assertNull(recovered.lab().operationalScore());
    }

    private static LabRubricSnapshot rubric(ChallengeRubric challenge) {
        return new LabRubricSnapshot(UUID.randomUUID(), Map.of(challenge.challengeNumber(), challenge));
    }

    private static ChallengeRubric challenge(UUID challengeId, UUID classId, UUID methodId, boolean hasMmd) {
        MethodRubric method = new MethodRubric(methodId, "run", "public", "void", false, false, false, List.of());
        ClassRubric classRubric = new ClassRubric(
                classId, "Box", "public", "class", false, List.of(), List.of(method), List.of(), 1);
        return new ChallengeRubric(
                challengeId, 1, "Challenge 1", List.of(classRubric), List.of(), List.of(),
                hasMmd, true, 1, 1, 1, 1);
    }
}
