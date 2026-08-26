package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.service.TestcaseRubricService;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInstanceRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestcaseRubricServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private TestcaseRepository testcaseRepository;
    @Mock private TestcaseInvocationRepository testcaseInvocationRepository;
    @Mock private TestcaseInstanceRepository testcaseInstanceRepository;
    @Mock private TestcaseAssertionRepository testcaseAssertionRepository;
    @Mock private LabRubricCache labRubricCache;
    @Mock private EntityManager entityManager;

    private final List<Testcase> storedTestcases = new ArrayList<>();
    private final List<TestcaseInvocation> storedInvocations = new ArrayList<>();
    private final List<TestcaseAssertion> storedAssertions = new ArrayList<>();

    private TestcaseRubricService service;

    private UUID labId;
    private UUID challengeId;
    private UUID classId;
    private UUID constructorId;
    private UUID methodId;
    private Challenge challenge;
    @Mock private Lab lab;
    private ClassEntity classEntity;
    private Constructor constructor;
    private Method method;

    @BeforeEach
    void setUp() {
        RubricCacheInvalidationSupport cacheSupport = new RubricCacheInvalidationSupport(labRubricCache);
        service = new TestcaseRubricService(
                challengeRepository,
                classEntityRepository,
                constructorRepository,
                methodRepository,
                fieldRepository,
                testcaseRepository,
                testcaseInvocationRepository,
                testcaseInstanceRepository,
                testcaseAssertionRepository,
                cacheSupport,
                entityManager);

        storedTestcases.clear();
        storedInvocations.clear();
        storedAssertions.clear();
        doAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof Testcase testcase) {
                storedTestcases.removeIf(existing -> existing.getId().equals(testcase.getId()));
                storedTestcases.add(testcase);
            } else if (arg instanceof TestcaseInvocation invocation) {
                storedInvocations.removeIf(existing -> existing.getId().equals(invocation.getId()));
                storedInvocations.add(invocation);
            } else if (arg instanceof TestcaseAssertion assertion) {
                storedAssertions.removeIf(existing -> existing.getId().equals(assertion.getId()));
                storedAssertions.add(assertion);
            }
            return null;
        }).when(entityManager).persist(any());
        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(any()))
                .thenAnswer(inv -> List.copyOf(storedTestcases));
        when(testcaseInvocationRepository.findByTestcase_IdIn(any()))
                .thenAnswer(inv -> List.copyOf(storedInvocations));
        when(testcaseAssertionRepository.findByTestcase_IdInOrderByOrderIndexAsc(any()))
                .thenAnswer(inv -> List.copyOf(storedAssertions));

        labId = UUID.randomUUID();
        challengeId = UUID.randomUUID();
        classId = UUID.randomUUID();
        constructorId = UUID.randomUUID();
        methodId = UUID.randomUUID();

        when(lab.getId()).thenReturn(labId);

        challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);

        classEntity = new ClassEntity();
        classEntity.setId(classId);
        classEntity.setChallenge(challenge);
        classEntity.setName("Car");

        constructor = new Constructor();
        constructor.setId(constructorId);
        constructor.setClassEntity(classEntity);

        method = new Method();
        method.setId(methodId);
        method.setClassEntity(classEntity);
        method.setName("getSpeed");
    }

    @Test
    void saveForChallenge_newClientId_insertsTestcase() {
        UUID clientTestcaseId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        TestcaseStructureDTO payload = singleInvocationDto(clientTestcaseId, invocationId, assertionId);

        stubChallengeAndMembers();
        when(testcaseRepository.findById(clientTestcaseId)).thenReturn(Optional.empty());
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testcaseInstanceRepository.findByTestcase_IdIn(any())).thenReturn(List.of());

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        verify(entityManager).persist(any(Testcase.class));
        verify(entityManager).persist(any(TestcaseInvocation.class));
        verify(entityManager, times(2)).flush();
        assertEquals(1, response.testcases().size());
        assertEquals(invocationId, response.testcases().get(0).invocation().id());
    }

    @Test
    void saveForChallenge_singleInvocation_persistsGraphAndInvalidatesCache() {
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        TestcaseStructureDTO payload = singleInvocationDto(null, invocationId, assertionId);

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testcaseInstanceRepository.findByTestcase_IdIn(any())).thenReturn(List.of());

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        verify(labRubricCache).invalidate(labId);
        verify(entityManager).persist(any(Testcase.class));
        verify(entityManager).persist(any(TestcaseInvocation.class));
        verify(entityManager).persist(any(TestcaseAssertion.class));
        assertEquals(1, response.testcases().size());
        assertEquals("deposit", response.testcases().get(0).name());
    }

    @Test
    void saveForChallenge_methodFromOtherChallenge_throws422() {
        UUID otherMethodId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                null,
                "bad",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                false,
                new InvocationStructureDTO(
                        UUID.randomUUID(),
                        InvocationKind.METHOD,
                        null,
                        otherMethodId,
                        "[]",
                        null,
                        "[]"),
                List.of(),
                List.of(new AssertionStructureDTO(
                        UUID.randomUUID(),
                        null,
                        AssertionKind.RETURN_VALUE,
                        null,
                        "0",
                        ComparisonMode.EXACT,
                        0)));

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(testcaseRepository, never()).save(any());
    }

    @Test
    void saveForChallenge_omittedTestcaseId_deletesExisting() {
        UUID existingId = UUID.randomUUID();
        Testcase existing = mock(Testcase.class);
        when(existing.getId()).thenReturn(existingId);
        when(existing.getChallenge()).thenReturn(challenge);

        stubChallengeAndMembers();
        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId))
                .thenReturn(List.of(existing))
                .thenReturn(List.of());
        when(testcaseInstanceRepository.findByTestcase_IdIn(any())).thenReturn(List.of());

        service.saveForChallenge(labId, challengeId, List.of());

        verify(testcaseRepository).delete(existing);
    }

    @Test
    void saveForChallenge_comparisonWithTwoInstances_succeeds() {
        UUID instanceAId = UUID.randomUUID();
        UUID instanceBId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                null,
                "compare",
                TestcaseType.COMPARISON,
                TestcaseComparisonMethod.EQUALS,
                1,
                0,
                false,
                null,
                List.of(
                        new InstanceStructureDTO(instanceAId, "A", constructorId, "[]"),
                        new InstanceStructureDTO(instanceBId, "B", constructorId, "[]")),
                List.of(new AssertionStructureDTO(
                        UUID.randomUUID(),
                        null,
                        AssertionKind.COMPARISON_RESULT,
                        null,
                        "true",
                        ComparisonMode.EXACT,
                        0)));

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testcaseInstanceRepository.findByTestcase_IdIn(any())).thenReturn(List.of());

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        verify(testcaseInstanceRepository, times(2)).save(any());
        assertEquals(1, response.testcases().size());
        assertEquals(TestcaseType.COMPARISON, response.testcases().get(0).testcaseType());
    }

    @Test
    void findReferencingTestcaseNames_methodInvocation_returnsNames() {
        UUID testcaseId = UUID.randomUUID();
        Testcase testcase = mock(Testcase.class);
        when(testcase.getId()).thenReturn(testcaseId);
        when(testcase.getName()).thenReturn("speed check");
        when(testcase.getChallenge()).thenReturn(challenge);

        TestcaseInvocation invocation = mock(TestcaseInvocation.class);
        when(invocation.getTestcase()).thenReturn(testcase);
        when(invocation.getMethod()).thenReturn(method);

        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId)).thenReturn(List.of(testcase));
        when(testcaseInvocationRepository.findByTestcase_IdIn(Set.of(testcaseId))).thenReturn(List.of(invocation));

        List<String> names = service.findReferencingTestcaseNames(
                challengeId, TestcaseRubricService.RubricMemberKind.METHOD, methodId);

        assertEquals(List.of("speed check"), names);
    }

    @Test
    void loadForChallenge_empty_returnsEmptyList() {
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId)).thenReturn(List.of());

        ChallengeTestcasesResponse response = service.loadForChallenge(labId, challengeId);

        assertTrue(response.testcases().isEmpty());
        assertEquals(challengeId, response.challengeId());
    }

    @Test
    void saveForChallenge_threeFieldStateAssertions_roundTripCount() {
        UUID fieldA = UUID.randomUUID();
        UUID fieldB = UUID.randomUUID();
        UUID fieldC = UUID.randomUUID();
        Field fa = new Field();
        fa.setId(fieldA);
        fa.setClassEntity(classEntity);
        Field fb = new Field();
        fb.setId(fieldB);
        fb.setClassEntity(classEntity);
        Field fc = new Field();
        fc.setId(fieldC);
        fc.setClassEntity(classEntity);

        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "integration-multi",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                100,
                false,
                new InvocationStructureDTO(invocationId, InvocationKind.CONSTRUCTOR, constructorId, null,
                        "[2020, \"Toyota\"]", null, "[]"),
                List.of(),
                List.of(
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldA, "2020", ComparisonMode.EXACT, 0),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldB, "\"Toyota\"", ComparisonMode.EXACT, 1),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldC, "0", ComparisonMode.EXACT, 2)));

        stubChallengeAndMembers();
        when(fieldRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(fa, fb, fc));
        when(fieldRepository.findById(fieldA)).thenReturn(Optional.of(fa));
        when(fieldRepository.findById(fieldB)).thenReturn(Optional.of(fb));
        when(fieldRepository.findById(fieldC)).thenReturn(Optional.of(fc));
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testcaseInstanceRepository.findByTestcase_IdIn(any())).thenReturn(List.of());

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        assertEquals(3, response.testcases().get(0).assertions().size());
    }

    private void stubChallengeAndMembers() {
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of(classEntity));
        when(constructorRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(constructor));
        when(methodRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(method));
        when(fieldRepository.findByClassEntityInWithDeclaration(List.of(classEntity))).thenReturn(List.of());
        when(constructorRepository.findById(constructorId)).thenReturn(Optional.of(constructor));
        when(methodRepository.findById(methodId)).thenReturn(Optional.of(method));
    }

    private TestcaseStructureDTO singleInvocationDto(UUID testcaseId, UUID invocationId, UUID assertionId) {
        return new TestcaseStructureDTO(
                testcaseId,
                "deposit",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                0,
                false,
                new InvocationStructureDTO(
                        invocationId,
                        InvocationKind.METHOD,
                        null,
                        methodId,
                        "[]",
                        constructorId,
                        "[]"),
                List.of(),
                List.of(new AssertionStructureDTO(
                        assertionId,
                        invocationId,
                        AssertionKind.RETURN_VALUE,
                        null,
                        "0",
                        ComparisonMode.EXACT,
                        0)));
    }
}
