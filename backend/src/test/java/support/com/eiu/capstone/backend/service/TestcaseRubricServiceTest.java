package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Collection;
import java.util.HashSet;
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
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.DryRunChallengeCatalogCache;
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
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.service.TestcaseRubricService;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class TestcaseRubricServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private ClassRelationRepository classRelationRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private TestcaseRepository testcaseRepository;
    @Mock private TestcaseInvocationRepository testcaseInvocationRepository;
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
    private UUID speedFieldId;
    private Field speedField;

    @BeforeEach
    void setUp() {
        DryRunChallengeCatalogCache catalogCache = new DryRunChallengeCatalogCache();
        RubricCacheInvalidationSupport cacheSupport = new RubricCacheInvalidationSupport(labRubricCache, catalogCache);
        when(classRelationRepository.findByClassEntityInWithEndpoints(any())).thenReturn(List.of());
        service = new TestcaseRubricService(
                challengeRepository,
                classEntityRepository,
                classRelationRepository,
                constructorRepository,
                methodRepository,
                fieldRepository,
                parameterRepository,
                testcaseRepository,
                testcaseInvocationRepository,
                testcaseAssertionRepository,
                cacheSupport,
                catalogCache,
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
                assertUniqueInvocationOrderIndexes();
            } else if (arg instanceof TestcaseAssertion assertion) {
                storedAssertions.removeIf(existing -> existing.getId().equals(assertion.getId()));
                storedAssertions.add(assertion);
            }
            return null;
        }).when(entityManager).persist(any());
        doAnswer(inv -> {
            assertUniqueInvocationOrderIndexes();
            return null;
        }).when(entityManager).flush();
        when(testcaseInvocationRepository.save(any(TestcaseInvocation.class))).thenAnswer(inv -> {
            TestcaseInvocation row = inv.getArgument(0);
            storedInvocations.removeIf(existing -> existing.getId().equals(row.getId()));
            storedInvocations.add(row);
            assertUniqueInvocationOrderIndexes();
            return row;
        });
        doAnswer(inv -> {
            TestcaseInvocation row = inv.getArgument(0);
            storedInvocations.removeIf(existing -> existing.getId().equals(row.getId()));
            return null;
        }).when(testcaseInvocationRepository).delete(any());
        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(any()))
                .thenAnswer(inv -> List.copyOf(storedTestcases));
        when(testcaseInvocationRepository.findByTestcase_IdIn(any()))
                .thenAnswer(inv -> {
                    Collection<UUID> ids = inv.getArgument(0);
                    return storedInvocations.stream()
                            .filter(row -> ids.contains(row.getTestcase().getId()))
                            .toList();
                });
        when(testcaseAssertionRepository.findByTestcase_IdInOrderByOrderIndexAsc(any()))
                .thenAnswer(inv -> {
                    Collection<UUID> ids = inv.getArgument(0);
                    return storedAssertions.stream()
                            .filter(row -> ids.contains(row.getTestcase().getId()))
                            .toList();
                });
        doAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            UUID id = inv.getArgument(1);
            if (type == Method.class) {
                return methodRepository.findById(id).orElse(null);
            }
            if (type == Constructor.class) {
                return constructorRepository.findById(id).orElse(null);
            }
            if (type == Field.class) {
                return fieldRepository.findById(id).orElse(null);
            }
            if (type == ClassEntity.class) {
                return classEntityRepository.findById(id).orElse(null);
            }
            return null;
        }).when(entityManager).getReference(any(), any());

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
        MethodDeclaration methodDeclaration = new MethodDeclaration();
        methodDeclaration.setReturnType("int");
        methodDeclaration.setStatic(false);
        method.setMethodDeclaration(methodDeclaration);

        speedFieldId = UUID.randomUUID();
        speedField = new Field();
        speedField.setId(speedFieldId);
        speedField.setClassEntity(classEntity);
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
        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        verify(entityManager).persist(any(Testcase.class));
        verify(entityManager).persist(any(TestcaseInvocation.class));
        verify(entityManager, times(4)).flush();
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
                TestcaseType.UNIT,
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
                        "[]",
                        null,
                        null),
                List.of(),
                List.of(new AssertionStructureDTO(
                        UUID.randomUUID(),
                        null,
                        AssertionKind.RETURN_VALUE,
                        null,
                        "0",
                        ComparisonMode.EXACT,
                        0)),
                null,
                null);

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
        service.saveForChallenge(labId, challengeId, List.of());

        verify(testcaseRepository).delete(existing);
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
    void findReferencingTestcaseNames_dispatchClass_returnsNames() {
        UUID testcaseId = UUID.randomUUID();
        Testcase testcase = mock(Testcase.class);
        when(testcase.getId()).thenReturn(testcaseId);
        when(testcase.getName()).thenReturn("poly dispatch");
        when(testcase.getChallenge()).thenReturn(challenge);

        TestcaseInvocation invocation = mock(TestcaseInvocation.class);
        when(invocation.getTestcase()).thenReturn(testcase);
        when(invocation.getDispatchClass()).thenReturn(classEntity);

        when(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId)).thenReturn(List.of(testcase));
        when(testcaseInvocationRepository.findByTestcase_IdIn(Set.of(testcaseId))).thenReturn(List.of(invocation));

        List<String> names = service.findReferencingTestcaseNames(
                challengeId, TestcaseRubricService.RubricMemberKind.CLASS, classId);

        assertEquals(List.of("poly dispatch"), names);
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
                TestcaseType.UNIT,
                null,
                1,
                100,
                false,
                new InvocationStructureDTO(invocationId, InvocationKind.CONSTRUCTOR, constructorId, null,
                        "[2020, \"Toyota\"]", null, "[]", null, null),
                List.of(),
                List.of(
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldA, "2020", ComparisonMode.EXACT, 0),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldB, "\"Toyota\"", ComparisonMode.EXACT, 1),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fieldC, "0", ComparisonMode.EXACT, 2)),
                null,
                null);

        stubChallengeAndMembers();
        when(fieldRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(fa, fb, fc));
        when(fieldRepository.findById(fieldA)).thenReturn(Optional.of(fa));
        when(fieldRepository.findById(fieldB)).thenReturn(Optional.of(fb));
        when(fieldRepository.findById(fieldC)).thenReturn(Optional.of(fc));
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        assertEquals(3, response.testcases().get(0).assertions().size());
    }

    @Test
    void validatePayload_unitMethod_doesNotThrow() {
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "poly-preview",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                methodInvocation(invocationId, null, null),
                List.of(),
                List.of(returnValueAssertion(UUID.randomUUID(), invocationId, "0")),
                null,
                OopPrincipleTag.Polymorphism);

        stubChallengeAndMembers();

        assertDoesNotThrow(() -> service.validatePayload(challengeId, payload));
    }

    @Test
    void saveForChallenge_oneInvocationOmittedTag_storesUnit() {
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        TestcaseStructureDTO payload = singleInvocationDto(UUID.randomUUID(), invocationId, assertionId);

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        assertEquals(1, response.testcases().size());
        assertEquals(invocationId, response.testcases().get(0).invocation().id());
        assertEquals(TestcaseType.UNIT, response.testcases().get(0).testcaseType());
        assertNull(response.testcases().get(0).oopPrincipleTag());
    }

    @Test
    void saveForChallenge_resave_keepsAssertionIds() {
        UUID testcaseId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        TestcaseStructureDTO first = singleInvocationDto(testcaseId, invocationId, assertionId);

        stubChallengeAndMembers();
        when(testcaseRepository.findById(testcaseId)).thenReturn(Optional.empty());
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testcaseInvocationRepository.save(any(TestcaseInvocation.class))).thenAnswer(inv -> {
            TestcaseInvocation row = inv.getArgument(0);
            storedInvocations.removeIf(existing -> existing.getId().equals(row.getId()));
            storedInvocations.add(row);
            return row;
        });
        when(testcaseAssertionRepository.save(any(TestcaseAssertion.class))).thenAnswer(inv -> {
            TestcaseAssertion row = inv.getArgument(0);
            storedAssertions.removeIf(existing -> existing.getId().equals(row.getId()));
            storedAssertions.add(row);
            return row;
        });
        service.saveForChallenge(labId, challengeId, List.of(first));
        assertEquals(1, storedAssertions.size());
        assertEquals(assertionId, storedAssertions.get(0).getId());

        when(testcaseRepository.findById(testcaseId)).thenAnswer(inv -> Optional.of(storedTestcases.get(0)));
        TestcaseStructureDTO second = new TestcaseStructureDTO(
                testcaseId,
                "deposit",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                methodInvocation(invocationId, null, null),
                List.of(),
                List.of(returnValueAssertion(assertionId, invocationId, "1")),
                null,
                null);

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(second));

        assertEquals(1, storedAssertions.size());
        assertEquals(assertionId, storedAssertions.get(0).getId());
        assertEquals("1", storedAssertions.get(0).getExpectedValue());
        assertEquals(assertionId, response.testcases().get(0).assertions().get(0).id());
        verify(testcaseAssertionRepository, never()).deleteAll(any());
        verify(testcaseAssertionRepository, never()).delete(any());
        verify(testcaseInvocationRepository, never()).deleteAll(any());
    }

    @Test
    void saveForChallenge_instanceRefUnknown_throws422() {
        UUID constructId = UUID.randomUUID();
        UUID consumeId = UUID.randomUUID();
        InvocationStructureDTO construct = constructorInvocation(constructId, "engine", "[]");
        InvocationStructureDTO consume = constructorInvocation(
                consumeId, "car", "[{\"$instance\":\"ghost\"}]");
        TestcaseStructureDTO payload = scenarioDto(
                construct,
                List.of(construct, consume),
                List.of(returnValueAssertion(UUID.randomUUID(), consumeId, "null")),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_instanceRefLaterName_throws422() {
        UUID laterId = UUID.randomUUID();
        UUID earlierId = UUID.randomUUID();
        InvocationStructureDTO usesLater = constructorInvocation(
                earlierId, "car", "[{\"$instance\":\"engine\"}]");
        InvocationStructureDTO later = constructorInvocation(laterId, "engine", "[]");
        TestcaseStructureDTO payload = scenarioDto(
                usesLater,
                List.of(usesLater, later),
                List.of(returnValueAssertion(UUID.randomUUID(), earlierId, "null")),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_overTwentySteps_throws422() {
        List<InvocationStructureDTO> steps = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            steps.add(constructorInvocation(UUID.randomUUID(), "step" + i, "[]"));
        }
        UUID firstId = steps.get(0).id();
        TestcaseStructureDTO payload = scenarioDto(
                steps.get(0),
                steps,
                List.of(returnValueAssertion(UUID.randomUUID(), firstId, "null")),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_overTenNamedInstances_throws422() {
        List<InvocationStructureDTO> steps = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            steps.add(constructorInvocation(UUID.randomUUID(), "obj" + i, "[]"));
        }
        UUID firstId = steps.get(0).id();
        TestcaseStructureDTO payload = scenarioDto(
                steps.get(0),
                steps,
                List.of(returnValueAssertion(UUID.randomUUID(), firstId, "null")),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_twoInvocations_roundTripOrderedSteps() {
        UUID ctorStepId = UUID.randomUUID();
        UUID methodStepId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        InvocationStructureDTO construct = constructorInvocation(ctorStepId, "car", "[]");
        InvocationStructureDTO call = methodInvocation(methodStepId, "car", null);
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "sequence",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(returnValueAssertion(assertionId, methodStepId, "0")),
                List.of(construct, call),
                null);

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        TestcaseStructureDTO saved = response.testcases().get(0);
        assertEquals(TestcaseType.COMPOSITION, saved.testcaseType());
        assertNotNull(saved.invocations());
        assertEquals(2, saved.invocations().size());
        assertEquals(ctorStepId, saved.invocations().get(0).id());
        assertEquals("car", saved.invocations().get(0).instanceName());
        assertEquals(methodStepId, saved.invocations().get(1).id());
        assertEquals("car", saved.invocations().get(1).instanceName());
        assertEquals(ctorStepId, saved.invocation().id());
        assertEquals(assertionId, saved.assertions().get(0).id());
        assertEquals(methodStepId, saved.assertions().get(0).invocationId());
    }

    @Test
    void saveForChallenge_omittingFirstStep_reindexesWithoutDuplicateOrder() {
        UUID testcaseId = UUID.randomUUID();
        UUID firstStepId = UUID.randomUUID();
        UUID secondStepId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        InvocationStructureDTO firstStep = constructorInvocation(firstStepId, "one", "[]");
        InvocationStructureDTO secondStep = constructorInvocation(secondStepId, "two", "[]");
        TestcaseStructureDTO first = new TestcaseStructureDTO(
                testcaseId,
                "sequence",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                firstStep,
                List.of(),
                List.of(fieldStateAssertion(assertionId, secondStepId, speedFieldId, "0")),
                List.of(firstStep, secondStep),
                null);

        stubChallengeAndMembers();
        when(testcaseRepository.findById(testcaseId)).thenReturn(Optional.empty());
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        service.saveForChallenge(labId, challengeId, List.of(first));
        when(testcaseRepository.findById(testcaseId)).thenReturn(Optional.of(storedTestcases.get(0)));

        TestcaseStructureDTO second = new TestcaseStructureDTO(
                testcaseId,
                "sequence",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                secondStep,
                List.of(),
                List.of(fieldStateAssertion(assertionId, secondStepId, speedFieldId, "0")),
                List.of(secondStep),
                null);

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(second));

        assertEquals(1, response.testcases().get(0).invocations().size());
        assertEquals(secondStepId, response.testcases().get(0).invocations().get(0).id());
        assertEquals(0, storedInvocations.get(0).getOrderIndex());
        assertEquals(1, storedInvocations.size());
    }

    @Test
    void saveForChallenge_unitInstanceMethodWithoutNoArg_saves() {
        UUID accountClassId = UUID.randomUUID();
        UUID accountCtorId = UUID.randomUUID();
        UUID withdrawId = UUID.randomUUID();
        ClassEntity account = new ClassEntity();
        account.setId(accountClassId);
        account.setChallenge(challenge);
        account.setName("Account");

        Constructor accountCtor = new Constructor();
        accountCtor.setId(accountCtorId);
        accountCtor.setClassEntity(account);

        Method withdraw = instanceMethod(withdrawId, account, "withdraw", "void");
        Parameter idParam = constructorParam(accountCtor, "id", "String", 0);

        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = unitMethodDto(
                "withdraw-unit",
                invocationId,
                withdrawId,
                "[10]",
                List.of(stdoutAssertion(UUID.randomUUID(), invocationId, "\"\"")));

        stubChallengeAndMembers();
        stubExtraMembers(List.of(classEntity, account), List.of(constructor, accountCtor),
                List.of(method, withdraw), List.of(idParam));
        when(methodRepository.findById(withdrawId)).thenReturn(Optional.of(withdraw));
        when(constructorRepository.findById(accountCtorId)).thenReturn(Optional.of(accountCtor));

        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        assertEquals(1, response.testcases().size());
        verify(entityManager).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_unitObjectTypedArgument_throws422() {
        UUID accountClassId = UUID.randomUUID();
        UUID transferId = UUID.randomUUID();
        ClassEntity account = new ClassEntity();
        account.setId(accountClassId);
        account.setChallenge(challenge);
        account.setName("Account");

        Method transfer = instanceMethod(transferId, classEntity, "transfer", "void");
        Parameter other = methodParam(transfer, "other", "Account", 0);
        Parameter amount = methodParam(transfer, "amount", "int", 1);

        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = unitMethodDto(
                "transfer-unit",
                invocationId,
                transferId,
                "[null, 100]",
                List.of(stdoutAssertion(UUID.randomUUID(), invocationId, "\"\"")));

        stubChallengeAndMembers();
        stubExtraMembers(List.of(classEntity, account), List.of(constructor),
                List.of(method, transfer), List.of());
        when(methodRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(parameterRepository.findByMethodIn(any())).thenReturn(List.of(other, amount));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_compositionSetupStepsWithoutAssertions_saves() {
        UUID firstCtorId = UUID.randomUUID();
        UUID secondCtorId = UUID.randomUUID();
        UUID methodStepId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();
        InvocationStructureDTO first = constructorInvocation(firstCtorId, "engine", "[]");
        InvocationStructureDTO second = constructorInvocation(secondCtorId, "car", "[]");
        InvocationStructureDTO call = methodInvocation(methodStepId, "car", null);
        TestcaseStructureDTO payload = compositionDto(
                List.of(first, second, call),
                List.of(returnValueAssertion(assertionId, methodStepId, "0")));

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        TestcaseStructureDTO saved = response.testcases().get(0);
        assertEquals(TestcaseType.COMPOSITION, saved.testcaseType());
        assertEquals(3, saved.invocations().size());
        assertEquals(1, saved.assertions().size());
        assertEquals(methodStepId, saved.assertions().get(0).invocationId());
        assertEquals("engine", saved.invocations().get(0).instanceName());
        assertEquals("car", saved.invocations().get(1).instanceName());
    }

    @Test
    void saveForChallenge_unitEqualsObjectCheck_throws422() {
        UUID invocationId = UUID.randomUUID();
        method.getMethodDeclaration().setReturnType("Car");
        TestcaseStructureDTO payload = unitMethodDto(
                "equals-unit",
                invocationId,
                methodId,
                "[]",
                List.of(returnValueAssertion(
                        UUID.randomUUID(),
                        invocationId,
                        "{\"$objectCheck\":\"EQUALS\",\"$instance\":\"other\"}")));

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_compositionNamedMethodReturn_thenInstanceRef_saves() {
        UUID factoryId = UUID.randomUUID();
        UUID consumeId = UUID.randomUUID();
        Method factory = staticMethod(factoryId, classEntity, "create", "Car");
        InvocationStructureDTO produce = namedMethodInvocation(factoryId, factoryId, "car", "[]");
        InvocationStructureDTO consume = constructorInvocation(
                consumeId, "copy", "[{\"$instance\":\"car\"}]");
        Parameter engineParam = constructorParam(constructor, "source", "Car", 0);
        TestcaseStructureDTO payload = compositionDto(
                List.of(produce, consume),
                List.of(fieldStateAssertion(UUID.randomUUID(), consumeId, speedFieldId, "0")));

        stubChallengeAndMembers();
        stubExtraMembers(List.of(classEntity), List.of(constructor), List.of(method, factory), List.of(engineParam));
        when(methodRepository.findById(factoryId)).thenReturn(Optional.of(factory));
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));

        ChallengeTestcasesResponse response = service.saveForChallenge(labId, challengeId, List.of(payload));

        assertEquals(TestcaseType.COMPOSITION, response.testcases().get(0).testcaseType());
        assertEquals(2, response.testcases().get(0).invocations().size());
        assertEquals("car", response.testcases().get(0).invocations().get(0).instanceName());
    }

    @Test
    void saveForChallenge_compositionUnknownMethodReturnName_throws422() {
        UUID factoryId = UUID.randomUUID();
        UUID consumeId = UUID.randomUUID();
        Method factory = staticMethod(factoryId, classEntity, "create", "Car");
        InvocationStructureDTO produce = namedMethodInvocation(factoryId, factoryId, "car", "[]");
        InvocationStructureDTO consume = constructorInvocation(
                consumeId, "copy", "[{\"$instance\":\"ghost\"}]");
        Parameter engineParam = constructorParam(constructor, "source", "Car", 0);
        TestcaseStructureDTO payload = compositionDto(
                List.of(produce, consume),
                List.of(fieldStateAssertion(UUID.randomUUID(), consumeId, speedFieldId, "0")));

        stubChallengeAndMembers();
        stubExtraMembers(List.of(classEntity), List.of(constructor), List.of(method, factory), List.of(engineParam));
        when(methodRepository.findById(factoryId)).thenReturn(Optional.of(factory));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_constructorReturnValue_throws422() {
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "ctor-return",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                constructorInvocation(invocationId, null, "[]"),
                List.of(),
                List.of(returnValueAssertion(UUID.randomUUID(), invocationId, "null")),
                List.of(),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("field state"));
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_fieldsObjectCheck_throws422() {
        UUID invocationId = UUID.randomUUID();
        method.getMethodDeclaration().setReturnType("Car");
        TestcaseStructureDTO payload = unitMethodDto(
                "fields-map",
                invocationId,
                methodId,
                "[]",
                List.of(returnValueAssertion(
                        UUID.randomUUID(),
                        invocationId,
                        "{\"$objectCheck\":\"FIELDS\",\"fields\":{\"speed\":0}}")));

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("field state"));
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_typeOnlyObjectCheck_throws422() {
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = unitMethodDto(
                "type-only",
                invocationId,
                methodId,
                "[]",
                List.of(returnValueAssertion(UUID.randomUUID(), invocationId, "{\"$objectCheck\":\"TYPE\"}")));

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_emptyAssertionList_throws422() {
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = unitMethodDto(
                "no-asserts", invocationId, methodId, "[]", List.of());

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_constructorStdoutAssertion_throws422() {
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "ctor-stdout",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                constructorInvocation(invocationId, null, "[]"),
                List.of(),
                List.of(stdoutAssertion(UUID.randomUUID(), invocationId, "\"hi\"")),
                List.of(),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_unitTwoInvocations_throws422() {
        UUID ctorStepId = UUID.randomUUID();
        UUID methodStepId = UUID.randomUUID();
        InvocationStructureDTO construct = constructorInvocation(ctorStepId, "car", "[]");
        InvocationStructureDTO call = methodInvocation(methodStepId, "car", null);
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "unit-multi",
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(returnValueAssertion(UUID.randomUUID(), methodStepId, "0")),
                List.of(construct, call),
                null);

        stubChallengeAndMembers();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void saveForChallenge_namedObjectArrayArgument_throws422() {
        UUID constructId = UUID.randomUUID();
        UUID consumeId = UUID.randomUUID();
        InvocationStructureDTO construct = constructorInvocation(constructId, "engine", "[]");
        InvocationStructureDTO consume = constructorInvocation(
                consumeId, "car", "[[{\"$instance\":\"engine\"}]]");
        Parameter enginesParam = constructorParam(constructor, "engines", "Car[]", 0);
        TestcaseStructureDTO payload = compositionDto(
                List.of(construct, consume),
                List.of(fieldStateAssertion(UUID.randomUUID(), consumeId, speedFieldId, "0")));

        stubChallengeAndMembers();
        stubExtraMembers(List.of(classEntity), List.of(constructor), List.of(method), List.of(enginesParam));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveForChallenge(labId, challengeId, List.of(payload)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(entityManager, never()).persist(any(Testcase.class));
    }

    @Test
    void loadForChallenge_roundTripsUnitAndComposition() {
        UUID unitInvocationId = UUID.randomUUID();
        UUID unitAssertionId = UUID.randomUUID();
        UUID ctorStepId = UUID.randomUUID();
        UUID methodStepId = UUID.randomUUID();
        TestcaseStructureDTO unit = unitMethodDto(
                "unit-hidden",
                unitInvocationId,
                methodId,
                "[]",
                List.of(returnValueAssertion(unitAssertionId, unitInvocationId, "0")));
        unit = new TestcaseStructureDTO(
                unit.id(),
                unit.name(),
                TestcaseType.UNIT,
                null,
                1,
                0,
                true,
                unit.invocation(),
                List.of(),
                unit.assertions(),
                List.of(unit.invocation()),
                null);
        InvocationStructureDTO construct = constructorInvocation(ctorStepId, "car", "[]");
        InvocationStructureDTO call = methodInvocation(methodStepId, "car", null);
        TestcaseStructureDTO composition = compositionDto(
                List.of(construct, call),
                List.of(returnValueAssertion(UUID.randomUUID(), methodStepId, "0")));

        stubChallengeAndMembers();
        when(testcaseRepository.save(any(Testcase.class))).thenAnswer(inv -> inv.getArgument(0));
        service.saveForChallenge(labId, challengeId, List.of(unit, composition));

        ChallengeTestcasesResponse loaded = service.loadForChallenge(labId, challengeId);

        assertEquals(2, loaded.testcases().size());
        TestcaseStructureDTO loadedUnit = loaded.testcases().stream()
                .filter(tc -> tc.testcaseType() == TestcaseType.UNIT)
                .findFirst()
                .orElseThrow();
        TestcaseStructureDTO loadedComposition = loaded.testcases().stream()
                .filter(tc -> tc.testcaseType() == TestcaseType.COMPOSITION)
                .findFirst()
                .orElseThrow();
        assertEquals(true, loadedUnit.hidden());
        assertEquals(1, loadedUnit.invocations().size());
        assertEquals(unitInvocationId, loadedUnit.invocations().get(0).id());
        assertEquals(2, loadedComposition.invocations().size());
        assertEquals("car", loadedComposition.invocations().get(0).instanceName());
    }

    private void stubChallengeAndMembers() {
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(challengeRepository.findAllById(any())).thenReturn(List.of(challenge));
        when(classEntityRepository.findByChallengeInWithAttributes(any())).thenReturn(List.of(classEntity));
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of(classEntity));
        when(constructorRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(constructor));
        when(methodRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(method));
        when(fieldRepository.findByClassEntityInWithDeclaration(List.of(classEntity)))
                .thenReturn(List.of(speedField));
        when(fieldRepository.findById(speedFieldId)).thenReturn(Optional.of(speedField));
        when(constructorRepository.findById(constructorId)).thenReturn(Optional.of(constructor));
        when(methodRepository.findById(methodId)).thenReturn(Optional.of(method));
        when(parameterRepository.findByConstructorEntityIn(any())).thenReturn(List.of());
        when(parameterRepository.findByMethodIn(any())).thenReturn(List.of());
        when(classEntityRepository.findById(classId)).thenReturn(Optional.of(classEntity));
    }

    private TestcaseStructureDTO singleInvocationDto(UUID testcaseId, UUID invocationId, UUID assertionId) {
        return new TestcaseStructureDTO(
                testcaseId,
                "deposit",
                TestcaseType.UNIT,
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
                        null,
                        "[]",
                        null,
                        null),
                List.of(),
                List.of(returnValueAssertion(assertionId, invocationId, "0")),
                null,
                null);
    }

    private TestcaseStructureDTO unitMethodDto(String name,
                                               UUID invocationId,
                                               UUID targetMethodId,
                                               String params,
                                               List<AssertionStructureDTO> assertions) {
        InvocationStructureDTO invocation = new InvocationStructureDTO(
                invocationId,
                InvocationKind.METHOD,
                null,
                targetMethodId,
                params,
                null,
                "[]",
                null,
                null);
        return new TestcaseStructureDTO(
                UUID.randomUUID(),
                name,
                TestcaseType.UNIT,
                null,
                1,
                0,
                false,
                invocation,
                List.of(),
                assertions,
                List.of(invocation),
                null);
    }

    private TestcaseStructureDTO compositionDto(List<InvocationStructureDTO> invocations,
                                                List<AssertionStructureDTO> assertions) {
        return new TestcaseStructureDTO(
                UUID.randomUUID(),
                "composition",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                invocations.get(0),
                List.of(),
                assertions,
                invocations,
                null);
    }

    private void stubExtraMembers(List<ClassEntity> classes,
                                  List<Constructor> constructors,
                                  List<Method> methods,
                                  List<Parameter> constructorParams) {
        when(classEntityRepository.findByChallengeInWithAttributes(any())).thenReturn(classes);
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(classes);
        when(constructorRepository.findByClassEntityInWithDeclaration(classes)).thenReturn(constructors);
        when(methodRepository.findByClassEntityInWithDeclaration(classes)).thenReturn(methods);
        when(parameterRepository.findByConstructorEntityIn(any())).thenReturn(constructorParams);
        for (ClassEntity cls : classes) {
            when(classEntityRepository.findById(cls.getId())).thenReturn(Optional.of(cls));
        }
    }

    private static Method instanceMethod(UUID id, ClassEntity owner, String name, String returnType) {
        Method row = new Method();
        row.setId(id);
        row.setClassEntity(owner);
        row.setName(name);
        MethodDeclaration declaration = new MethodDeclaration();
        declaration.setReturnType(returnType);
        declaration.setStatic(false);
        row.setMethodDeclaration(declaration);
        return row;
    }

    private static Method staticMethod(UUID id, ClassEntity owner, String name, String returnType) {
        Method row = instanceMethod(id, owner, name, returnType);
        row.getMethodDeclaration().setStatic(true);
        return row;
    }

    private static Parameter constructorParam(Constructor constructor, String name, String dataType, int order) {
        Parameter parameter = new Parameter();
        parameter.setConstructorEntity(constructor);
        parameter.setName(name);
        parameter.setDataType(dataType);
        parameter.setOrderIndex(order);
        return parameter;
    }

    private static Parameter methodParam(Method owner, String name, String dataType, int order) {
        Parameter parameter = new Parameter();
        parameter.setMethod(owner);
        parameter.setName(name);
        parameter.setDataType(dataType);
        parameter.setOrderIndex(order);
        return parameter;
    }

    private InvocationStructureDTO namedMethodInvocation(UUID id, UUID targetMethodId, String instanceName, String params) {
        return new InvocationStructureDTO(
                id,
                InvocationKind.METHOD,
                null,
                targetMethodId,
                params,
                null,
                "[]",
                instanceName,
                null);
    }

    private static AssertionStructureDTO stdoutAssertion(UUID id, UUID invocationId, String expected) {
        return new AssertionStructureDTO(
                id,
                invocationId,
                AssertionKind.STDOUT,
                null,
                expected,
                ComparisonMode.EXACT,
                0);
    }

    private TestcaseStructureDTO scenarioDto(InvocationStructureDTO singular,
                                             List<InvocationStructureDTO> invocations,
                                             List<AssertionStructureDTO> assertions,
                                             OopPrincipleTag tag) {
        return new TestcaseStructureDTO(
                UUID.randomUUID(),
                "scenario",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                singular,
                List.of(),
                assertions,
                invocations,
                tag);
    }

    private InvocationStructureDTO constructorInvocation(UUID id, String instanceName, String params) {
        return constructorInvocation(id, instanceName, params, null);
    }

    private InvocationStructureDTO constructorInvocation(
            UUID id, String instanceName, String params, UUID dispatchClassId) {
        return new InvocationStructureDTO(
                id,
                InvocationKind.CONSTRUCTOR,
                constructorId,
                null,
                params,
                null,
                "[]",
                instanceName,
                dispatchClassId);
    }

    private InvocationStructureDTO methodInvocation(UUID id, String instanceName, UUID dispatchClassId) {
        return new InvocationStructureDTO(
                id,
                InvocationKind.METHOD,
                null,
                methodId,
                "[]",
                null,
                "[]",
                instanceName,
                dispatchClassId);
    }

    private static AssertionStructureDTO returnValueAssertion(UUID id, UUID invocationId, String expected) {
        return new AssertionStructureDTO(
                id,
                invocationId,
                AssertionKind.RETURN_VALUE,
                null,
                expected,
                ComparisonMode.EXACT,
                0);
    }

    private static AssertionStructureDTO fieldStateAssertion(
            UUID id, UUID invocationId, UUID fieldId, String expected) {
        return new AssertionStructureDTO(
                id,
                invocationId,
                AssertionKind.FIELD_STATE,
                fieldId,
                expected,
                ComparisonMode.EXACT,
                0);
    }

    private void assertUniqueInvocationOrderIndexes() {
        Set<String> seen = new HashSet<>();
        for (TestcaseInvocation row : storedInvocations) {
            String key = row.getTestcase().getId() + ":" + row.getOrderIndex();
            if (!seen.add(key)) {
                throw new IllegalStateException(
                        "duplicate order_index " + row.getOrderIndex() + " for testcase " + row.getTestcase().getId());
            }
        }
    }
}
