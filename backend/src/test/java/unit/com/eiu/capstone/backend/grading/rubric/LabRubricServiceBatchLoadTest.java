package unit.com.eiu.capstone.backend.grading.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.rubric.LabRubricService;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

@ExtendWith(MockitoExtension.class)
class LabRubricServiceBatchLoadTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private ClassRelationRepository classRelationRepository;
    @Mock private TestcaseRepository testcaseRepository;
    @Mock private TestcaseInvocationRepository testcaseInvocationRepository;
    @Mock private TestcaseAssertionRepository testcaseAssertionRepository;

    private LabRubricService service;

    @BeforeEach
    void setUp() {
        service = new LabRubricService(
                challengeRepository,
                classEntityRepository,
                fieldRepository,
                methodRepository,
                constructorRepository,
                parameterRepository,
                classRelationRepository,
                testcaseRepository,
                testcaseInvocationRepository,
                testcaseAssertionRepository);
    }

    @Test
    void loadForLabs_usesOneChallengeQuery_forAllLabs() {
        Lab labA = lab();
        Lab labB = lab();
        Challenge challengeA = challenge(labA, 1);
        Challenge challengeB = challenge(labB, 1);

        when(challengeRepository.findByLab_IdInOrderByChallengeNumberAsc(anyList()))
                .thenReturn(List.of(challengeA, challengeB));
        when(classEntityRepository.findByChallengeInWithAttributes(anyList())).thenReturn(List.of());
        when(testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(anyList())).thenReturn(List.of());

        Map<UUID, LabRubricSnapshot> snapshots = service.loadForLabs(List.of(labA, labB));

        assertEquals(2, snapshots.size());
        assertTrue(snapshots.get(labA.getId()).byChallengeNumber().containsKey(1));
        assertTrue(snapshots.get(labB.getId()).byChallengeNumber().containsKey(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> labIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(challengeRepository, times(1)).findByLab_IdInOrderByChallengeNumberAsc(labIdsCaptor.capture());
        assertEquals(2, labIdsCaptor.getValue().size());
        verify(challengeRepository, never()).findByLabOrderByChallengeNumberAsc(labA);
        verify(challengeRepository, never()).findByLabOrderByChallengeNumberAsc(labB);
        verify(classEntityRepository, times(1)).findByChallengeInWithAttributes(anyList());
    }

    @Test
    void loadForLabs_emptyLab_getsEmptySnapshot() {
        Lab empty = lab();
        when(challengeRepository.findByLab_IdInOrderByChallengeNumberAsc(anyList())).thenReturn(List.of());

        Map<UUID, LabRubricSnapshot> snapshots = service.loadForLabs(List.of(empty));

        assertEquals(1, snapshots.size());
        assertTrue(snapshots.get(empty.getId()).byChallengeNumber().isEmpty());
        verify(classEntityRepository, never()).findByChallengeInWithAttributes(anyList());
    }

    private static Lab lab() {
        Lab lab = new Lab();
        lab.setId(UUID.randomUUID());
        lab.setName("Lab");
        return lab;
    }

    private static Challenge challenge(Lab lab, int number) {
        Challenge challenge = new Challenge();
        challenge.setId(UUID.randomUUID());
        challenge.setName("C" + number);
        challenge.setLab(lab);
        challenge.setChallengeNumber(number);
        return challenge;
    }
}
