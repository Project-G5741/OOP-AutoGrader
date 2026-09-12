package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private ClassRelationRepository classRelationRepository;
    @Mock private SubmissionChallengeResultRepository submissionChallengeResultRepository;
    @Mock private SubmissionResolutionService submissionResolutionService;
    @Mock private SubmissionResultLoader submissionResultLoader;

    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        challengeService = new ChallengeService(
                challengeRepository,
                classEntityRepository,
                fieldRepository,
                methodRepository,
                constructorRepository,
                classRelationRepository,
                submissionChallengeResultRepository,
                submissionResolutionService,
                submissionResultLoader,
                false);
    }

    @Test
    void listSidebarChallengesByLabIds_groupsWithoutScores() {
        UUID labA = UUID.randomUUID();
        UUID labB = UUID.randomUUID();
        Challenge first = challenge("One", 1, labA);
        Challenge second = challenge("Two", 2, labA);
        Challenge other = challenge("Other", 1, labB);
        when(challengeRepository.findByLab_IdInOrderByChallengeNumberAsc(List.of(labA, labB)))
                .thenReturn(List.of(first, second, other));

        Map<UUID, List<ChallengeDTO>> byLab = challengeService.listSidebarChallengesByLabIds(List.of(labA, labB));

        assertEquals(2, byLab.get(labA).size());
        assertEquals("One", byLab.get(labA).get(0).name());
        assertNull(byLab.get(labA).get(0).score());
        assertEquals(1, byLab.get(labB).size());
        assertEquals("Other", byLab.get(labB).get(0).name());
    }

    @Test
    void listSidebarChallengesByLabIds_emptyInput_skipsQuery() {
        assertTrue(challengeService.listSidebarChallengesByLabIds(List.of()).isEmpty());
        verifyNoInteractions(challengeRepository);
    }

    private static Challenge challenge(String name, int number, UUID labId) {
        Lab lab = new Lab();
        setId(Lab.class, lab, labId);
        Challenge challenge = new Challenge();
        setId(Challenge.class, challenge, UUID.randomUUID());
        challenge.setName(name);
        challenge.setChallengeNumber(number);
        challenge.setLab(lab);
        challenge.setHasMmd(true);
        challenge.setWeight(1);
        challenge.setClassWeight(1);
        challenge.setMmdWeight(1);
        challenge.setTestcaseWeight(1);
        return challenge;
    }

    private static void setId(Class<?> type, Object target, UUID id) {
        try {
            var field = type.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
