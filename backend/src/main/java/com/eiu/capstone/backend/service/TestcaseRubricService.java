package com.eiu.capstone.backend.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.grading.rubric.RubricParameterMaps;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ClassRelation;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInstance;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseType;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TestcaseRubricService {

    public enum RubricMemberKind {
        METHOD, CONSTRUCTOR, FIELD, CLASS
    }

    static final int MAX_STEPS = 20;
    static final int MAX_NAMED_INSTANCES = 10;
    private static final int ORDER_INDEX_PARK = MAX_STEPS;
    private static final String INSTANCE_REF_KEY = "$instance";
    private static final String OBJECT_CHECK_KEY = "$objectCheck";
    private static final String OBJECT_CHECK_TYPE = "TYPE";
    private static final String OBJECT_CHECK_FIELDS = "FIELDS";
    private static final String OBJECT_CHECK_EQUALS = "EQUALS";

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassRelationRepository classRelationRepository;
    private final ConstructorRepository constructorRepository;
    private final MethodRepository methodRepository;
    private final FieldRepository fieldRepository;
    private final ParameterRepository parameterRepository;
    private final TestcaseRepository testcaseRepository;
    private final TestcaseInvocationRepository testcaseInvocationRepository;
    private final TestcaseAssertionRepository testcaseAssertionRepository;
    private final RubricCacheInvalidationSupport rubricCacheInvalidationSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityManager entityManager;

    public TestcaseRubricService(ChallengeRepository challengeRepository,
                                   ClassEntityRepository classEntityRepository,
                                   ClassRelationRepository classRelationRepository,
                                   ConstructorRepository constructorRepository,
                                   MethodRepository methodRepository,
                                   FieldRepository fieldRepository,
                                   ParameterRepository parameterRepository,
                                   TestcaseRepository testcaseRepository,
                                   TestcaseInvocationRepository testcaseInvocationRepository,
                                   TestcaseAssertionRepository testcaseAssertionRepository,
                                   RubricCacheInvalidationSupport rubricCacheInvalidationSupport,
                                   EntityManager entityManager) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.classRelationRepository = classRelationRepository;
        this.constructorRepository = constructorRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.parameterRepository = parameterRepository;
        this.testcaseRepository = testcaseRepository;
        this.testcaseInvocationRepository = testcaseInvocationRepository;
        this.testcaseAssertionRepository = testcaseAssertionRepository;
        this.rubricCacheInvalidationSupport = rubricCacheInvalidationSupport;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public ChallengeTestcasesResponse loadForChallenge(UUID labId, UUID challengeId) {
        Challenge challenge = requireChallengeInLab(labId, challengeId);
        List<TestcaseStructureDTO> testcases = loadDtosForChallenge(challenge.getId());
        return new ChallengeTestcasesResponse(labId, challengeId, testcases);
    }

    /**
     * Batch-load OT DTOs for many challenges in a few queries (clone/read paths).
     * Keys with no testcases are omitted from the map.
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<TestcaseStructureDTO>> loadDtosGroupedByChallengeIds(Collection<UUID> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = challengeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Testcase> testcases = testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(ids);
        if (testcases.isEmpty()) {
            return Map.of();
        }
        Set<UUID> testcaseIds = testcases.stream().map(Testcase::getId).collect(Collectors.toSet());
        Map<UUID, List<TestcaseInvocation>> invocationsByTestcaseId = testcaseInvocationRepository
                .findByTestcase_IdIn(testcaseIds).stream()
                .collect(Collectors.groupingBy(inv -> inv.getTestcase().getId()));
        Map<UUID, List<TestcaseAssertion>> assertionsByTestcaseId = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds).stream()
                .collect(Collectors.groupingBy(a -> a.getTestcase().getId()));

        Map<UUID, List<TestcaseStructureDTO>> byChallenge = new LinkedHashMap<>();
        for (Testcase tc : testcases) {
            UUID challengeId = tc.getChallenge().getId();
            TestcaseStructureDTO dto = toDto(
                    tc,
                    invocationsByTestcaseId.getOrDefault(tc.getId(), List.of()),
                    List.of(),
                    assertionsByTestcaseId.getOrDefault(tc.getId(), List.of()));
            byChallenge.computeIfAbsent(challengeId, ignored -> new ArrayList<>()).add(dto);
        }
        return byChallenge;
    }

    /**
     * Delete all OT rows for the given challenges (invocations/assertions then testcases).
     * Set-based SQL — prefer over per-testcase entity deletes on Neon.
     */
    @Transactional
    public void deleteAllForChallenges(Collection<UUID> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return;
        }
        List<UUID> ids = challengeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        entityManager.createNativeQuery("""
                        DELETE FROM testcase_assertion
                        WHERE testcase_id IN (SELECT id FROM testcase WHERE challenge_id IN (:challengeIds))
                        """)
                .setParameter("challengeIds", ids)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM testcase_invocation
                        WHERE testcase_id IN (SELECT id FROM testcase WHERE challenge_id IN (:challengeIds))
                        """)
                .setParameter("challengeIds", ids)
                .executeUpdate();
        entityManager.createNativeQuery(
                        "DELETE FROM testcase WHERE challenge_id IN (:challengeIds)")
                .setParameter("challengeIds", ids)
                .executeUpdate();
        entityManager.flush();
    }

    @Transactional(readOnly = true)
    public void validatePayload(UUID challengeId, TestcaseStructureDTO dto) {
        validateTestcaseDto(dto, loadChallengeMemberIds(challengeId));
    }

    public static List<InvocationStructureDTO> resolvedInvocations(TestcaseStructureDTO dto) {
        if (dto.invocations() != null && !dto.invocations().isEmpty()) {
            return dto.invocations();
        }
        if (dto.invocation() != null) {
            return List.of(dto.invocation());
        }
        return List.of();
    }

    @Transactional
    public ChallengeTestcasesResponse saveForChallenge(UUID labId,
                                                       UUID challengeId,
                                                       List<TestcaseStructureDTO> testcases) {
        persistTestcases(labId, challengeId, testcases, true);
        return new ChallengeTestcasesResponse(labId, challengeId, loadDtosForChallenge(challengeId));
    }

    /**
     * Insert-only OT write for a freshly cloned lab. Skips find/delete/reload/invalidate and
     * defers flush so Neon does not pay a round-trip per entity.
     * Keys are the new challenge ids already saved into {@code labId}.
     */
    @Transactional
    public void persistClonedTestcasesBatch(UUID labId, Map<UUID, List<TestcaseStructureDTO>> byChallenge) {
        if (byChallenge == null || byChallenge.isEmpty()) {
            return;
        }
        Map<UUID, List<TestcaseStructureDTO>> payloads = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<TestcaseStructureDTO>> entry : byChallenge.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            List<TestcaseStructureDTO> rows = entry.getValue() != null ? entry.getValue() : List.of();
            if (!rows.isEmpty()) {
                payloads.put(entry.getKey(), rows);
            }
        }
        if (payloads.isEmpty()) {
            return;
        }

        // saveLabStructure may still hold unflushed inserts in this same TX; IN queries
        // would miss them without an explicit flush.
        entityManager.flush();

        List<Challenge> challenges = challengeRepository.findAllById(payloads.keySet());
        if (challenges.size() != payloads.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found");
        }
        Map<UUID, Challenge> byId = new HashMap<>();
        for (Challenge challenge : challenges) {
            if (challenge.getLab() == null || !labId.equals(challenge.getLab().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found in lab");
            }
            byId.put(challenge.getId(), challenge);
        }

        // One batched member-id load for the whole lab — not per-challenge Neon round-trips.
        Map<UUID, ChallengeMemberIds> memberIdsByChallenge =
                loadChallengeMemberIdsGrouped(payloads.keySet());

        for (Map.Entry<UUID, List<TestcaseStructureDTO>> entry : payloads.entrySet()) {
            Challenge challenge = byId.get(entry.getKey());
            ChallengeMemberIds memberIds = memberIdsByChallenge.get(entry.getKey());
            if (memberIds == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found");
            }
            for (TestcaseStructureDTO dto : entry.getValue()) {
                validateTestcaseDto(dto, memberIds);
                insertClonedTestcaseGraph(challenge, dto, memberIds);
            }
        }
        entityManager.flush();
    }

    private void insertClonedTestcaseGraph(Challenge challenge,
                                           TestcaseStructureDTO dto,
                                           ChallengeMemberIds memberIds) {
        UUID testcaseId = dto.id() != null ? dto.id() : UUID.randomUUID();
        Testcase testcase = new Testcase();
        testcase.setId(testcaseId);
        testcase.setChallenge(challenge);
        testcase.setName(dto.name().trim());
        testcase.setTestcaseType(dto.testcaseType());
        testcase.setOrderIndex(dto.orderIndex());
        testcase.setHidden(dto.hidden());
        entityManager.persist(testcase);

        List<InvocationStructureDTO> steps = resolvedInvocations(dto);
        Map<UUID, TestcaseInvocation> invocationsById = new LinkedHashMap<>();
        int orderIndex = 0;
        for (InvocationStructureDTO step : steps) {
            UUID invocationId = step.id() != null ? step.id() : UUID.randomUUID();
            TestcaseInvocation invocation = new TestcaseInvocation();
            invocation.setId(invocationId);
            invocation.setTestcase(testcase);
            applyInvocation(invocation, step, orderIndex++, memberIds);
            entityManager.persist(invocation);
            invocationsById.put(invocationId, invocation);
        }

        if (dto.assertions() == null || dto.assertions().isEmpty()) {
            return;
        }
        for (AssertionStructureDTO assertionDto : dto.assertions()) {
            UUID assertionId = assertionDto.id() != null ? assertionDto.id() : UUID.randomUUID();
            TestcaseAssertion assertion = new TestcaseAssertion();
            assertion.setId(assertionId);
            assertion.setTestcase(testcase);
            assertion.setAssertionKind(assertionDto.assertionKind());
            assertion.setExpectedValue(normalizeExpectedValue(assertionDto.expectedValue()));
            assertion.setComparisonMode(
                    assertionDto.comparisonMode() != null ? assertionDto.comparisonMode() : ComparisonMode.EXACT);
            assertion.setOrderIndex(assertionDto.orderIndex());
            if (assertionDto.assertionKind() == AssertionKind.FIELD_STATE) {
                assertion.setField(requireField(assertionDto.fieldId(), memberIds));
            } else {
                assertion.setField(null);
            }
            if (assertionDto.invocationId() != null && invocationsById.containsKey(assertionDto.invocationId())) {
                assertion.setInvocation(invocationsById.get(assertionDto.invocationId()));
            } else {
                assertion.setInvocation(null);
            }
            entityManager.persist(assertion);
        }
    }

    private void persistTestcases(UUID labId,
                                  UUID challengeId,
                                  List<TestcaseStructureDTO> payloads,
                                  boolean invalidateCache) {
        Challenge challenge = requireChallengeInLab(labId, challengeId);
        ChallengeMemberIds memberIds = loadChallengeMemberIds(challenge.getId());
        List<TestcaseStructureDTO> testcasePayloads = payloads != null ? payloads : List.of();

        Set<UUID> payloadIds = testcasePayloads.stream()
                .map(TestcaseStructureDTO::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        long idsWithValue = testcasePayloads.stream()
                .map(TestcaseStructureDTO::id)
                .filter(Objects::nonNull)
                .count();
        if (idsWithValue != payloadIds.size()) {
            throw unprocessable("Duplicate testcase IDs in payload");
        }

        List<Testcase> existing = testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challenge.getId());
        for (Testcase existingRow : existing) {
            if (!payloadIds.contains(existingRow.getId())) {
                deleteTestcaseGraph(existingRow.getId());
                testcaseRepository.delete(existingRow);
            }
        }

        for (TestcaseStructureDTO dto : testcasePayloads) {
            validateTestcaseDto(dto, memberIds);
            Testcase testcase = upsertTestcase(challenge, dto);
            Map<UUID, TestcaseInvocation> invocations = syncInvocations(
                    testcase, resolvedInvocations(dto), memberIds);
            syncInstances(testcase, dto.instances(), memberIds);
            syncAssertions(testcase, dto.assertions(), memberIds, invocations);
        }

        if (invalidateCache) {
            rubricCacheInvalidationSupport.invalidateLab(labId);
        }
    }

    @Transactional(readOnly = true)
    public List<String> findReferencingTestcaseNames(UUID challengeId,
                                                     RubricMemberKind kind,
                                                     UUID memberId) {
        if (memberId == null) {
            return List.of();
        }
        List<Testcase> testcases = testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId);
        if (testcases.isEmpty()) {
            return List.of();
        }
        Set<UUID> testcaseIds = testcases.stream().map(Testcase::getId).collect(Collectors.toSet());
        Map<UUID, String> namesById = testcases.stream()
                .collect(Collectors.toMap(Testcase::getId, Testcase::getName));

        Set<UUID> referenced = new HashSet<>();
        if (kind == RubricMemberKind.METHOD) {
            testcaseInvocationRepository.findByTestcase_IdIn(testcaseIds).stream()
                    .filter(inv -> inv.getMethod() != null && memberId.equals(inv.getMethod().getId()))
                    .forEach(inv -> referenced.add(inv.getTestcase().getId()));
        } else if (kind == RubricMemberKind.CONSTRUCTOR) {
            testcaseInvocationRepository.findByTestcase_IdIn(testcaseIds).stream()
                    .filter(inv -> (inv.getConstructor() != null && memberId.equals(inv.getConstructor().getId()))
                            || (inv.getReceiverConstructor() != null
                            && memberId.equals(inv.getReceiverConstructor().getId())))
                    .forEach(inv -> referenced.add(inv.getTestcase().getId()));
        } else if (kind == RubricMemberKind.FIELD) {
            testcaseAssertionRepository.findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds).stream()
                    .filter(a -> a.getField() != null && memberId.equals(a.getField().getId()))
                    .forEach(a -> referenced.add(a.getTestcase().getId()));
        } else if (kind == RubricMemberKind.CLASS) {
            testcaseInvocationRepository.findByTestcase_IdIn(testcaseIds).stream()
                    .filter(inv -> inv.getDispatchClass() != null
                            && memberId.equals(inv.getDispatchClass().getId()))
                    .forEach(inv -> referenced.add(inv.getTestcase().getId()));
        }

        return referenced.stream()
                .map(namesById::get)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private Challenge requireChallengeInLab(UUID labId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));
        if (!challenge.getLab().getId().equals(labId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found in lab");
        }
        return challenge;
    }

    private List<TestcaseStructureDTO> loadDtosForChallenge(UUID challengeId) {
        List<Testcase> testcases = testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId);
        if (testcases.isEmpty()) {
            return List.of();
        }
        Set<UUID> testcaseIds = testcases.stream().map(Testcase::getId).collect(Collectors.toSet());

        Map<UUID, List<TestcaseInvocation>> invocationsByTestcaseId = testcaseInvocationRepository
                .findByTestcase_IdIn(testcaseIds).stream()
                .collect(Collectors.groupingBy(inv -> inv.getTestcase().getId()));

        Map<UUID, List<TestcaseInstance>> instancesByTestcaseId = Map.of();

        Map<UUID, List<TestcaseAssertion>> assertionsByTestcaseId = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds).stream()
                .collect(Collectors.groupingBy(a -> a.getTestcase().getId()));

        return testcases.stream()
                .map(tc -> toDto(
                        tc,
                        invocationsByTestcaseId.getOrDefault(tc.getId(), List.of()),
                        instancesByTestcaseId.getOrDefault(tc.getId(), List.of()),
                        assertionsByTestcaseId.getOrDefault(tc.getId(), List.of())))
                .toList();
    }

    private TestcaseStructureDTO toDto(Testcase testcase,
                                       List<TestcaseInvocation> invocations,
                                       List<TestcaseInstance> instances,
                                       List<TestcaseAssertion> assertions) {
        List<InvocationStructureDTO> invocationDtos = invocations.stream()
                .sorted(Comparator.comparingInt(TestcaseInvocation::getOrderIndex))
                .map(this::toInvocationDto)
                .toList();
        InvocationStructureDTO singular = invocationDtos.isEmpty() ? null : invocationDtos.get(0);

        List<InstanceStructureDTO> instanceDtos = instances.stream()
                .sorted(Comparator.comparing(TestcaseInstance::getLabel))
                .map(inst -> new InstanceStructureDTO(
                        inst.getId(),
                        inst.getLabel(),
                        inst.getConstructor().getId(),
                        inst.getParams()))
                .toList();

        List<AssertionStructureDTO> assertionDtos = assertions.stream()
                .sorted(Comparator.comparingInt(TestcaseAssertion::getOrderIndex))
                .map(a -> new AssertionStructureDTO(
                        a.getId(),
                        a.getInvocation() != null ? a.getInvocation().getId() : null,
                        a.getAssertionKind(),
                        a.getField() != null ? a.getField().getId() : null,
                        a.getExpectedValue(),
                        a.getComparisonMode(),
                        a.getOrderIndex()))
                .toList();

        return new TestcaseStructureDTO(
                testcase.getId(),
                testcase.getName(),
                testcase.getTestcaseType(),
                null,
                1,
                testcase.getOrderIndex(),
                testcase.isHidden(),
                singular,
                instanceDtos.isEmpty() ? List.of() : instanceDtos,
                assertionDtos,
                invocationDtos,
                null);
    }

    private InvocationStructureDTO toInvocationDto(TestcaseInvocation invocation) {
        return new InvocationStructureDTO(
                invocation.getId(),
                invocation.getInvocationKind(),
                invocation.getConstructor() != null ? invocation.getConstructor().getId() : null,
                invocation.getMethod() != null ? invocation.getMethod().getId() : null,
                invocation.getParams(),
                invocation.getReceiverConstructor() != null
                        ? invocation.getReceiverConstructor().getId() : null,
                invocation.getReceiverParams(),
                invocation.getInstanceName(),
                invocation.getDispatchClass() != null ? invocation.getDispatchClass().getId() : null);
    }

    private void validateTestcaseDto(TestcaseStructureDTO dto, ChallengeMemberIds memberIds) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw unprocessable("Testcase name is required");
        }
        if (dto.testcaseType() == null) {
            throw unprocessable("Testcase type is required");
        }
        if (dto.testcaseType() != TestcaseType.UNIT && dto.testcaseType() != TestcaseType.COMPOSITION) {
            throw unprocessable("Testcase type must be UNIT or COMPOSITION");
        }
        List<InvocationStructureDTO> steps = resolvedInvocations(dto);
        if (dto.instances() != null && !dto.instances().isEmpty()) {
            throw unprocessable("UNIT and COMPOSITION must not have instances");
        }
        if (dto.assertions() == null || dto.assertions().isEmpty()) {
            throw unprocessable("At least one assertion is required");
        }
        Map<String, String> namedInstances;
        if (dto.testcaseType() == TestcaseType.UNIT) {
            namedInstances = validateUnitSteps(steps, memberIds);
        } else {
            namedInstances = validateCompositionSteps(steps, memberIds);
        }
        validateAssertions(dto, steps, memberIds, namedInstances);
    }

    private Map<String, String> validateUnitSteps(List<InvocationStructureDTO> steps,
                                                  ChallengeMemberIds memberIds) {
        if (steps.size() != 1) {
            throw unprocessable("UNIT requires exactly one invocation");
        }
        InvocationStructureDTO step = steps.get(0);
        validateInvocation(step, memberIds, true);
        if (blankToNull(step.instanceName()) != null) {
            throw unprocessable("UNIT cannot name instances");
        }
        if (step.receiverConstructorId() != null) {
            throw unprocessable("UNIT cannot set receiver_constructor_id");
        }
        if (containsInstanceRef(step.params()) || containsInstanceRef(step.receiverParams())) {
            throw unprocessable("UNIT cannot pass named instances as arguments");
        }
        List<String> paramTypes = paramTypesFor(step, memberIds);
        if (hasRubricClassArgument(paramTypes, memberIds)) {
            throw unprocessable("UNIT cannot pass a rubric-class object as an argument");
        }
        rejectObjectArrayArgs(step.params(), paramTypes, memberIds, "Invocation params");
        return Map.of();
    }

    private Map<String, String> validateCompositionSteps(List<InvocationStructureDTO> steps,
                                                         ChallengeMemberIds memberIds) {
        if (steps.isEmpty()) {
            throw unprocessable("COMPOSITION requires at least one invocation");
        }
        if (steps.size() > MAX_STEPS) {
            throw unprocessable("A testcase may have at most " + MAX_STEPS + " steps");
        }
        Map<String, String> named = new LinkedHashMap<>();
        Set<String> usedNames = new HashSet<>();
        int namedCount = 0;
        for (InvocationStructureDTO step : steps) {
            validateInvocation(step, memberIds, false);
            if (step.invocationKind() == InvocationKind.CONSTRUCTOR) {
                String name = blankToNull(step.instanceName());
                if (name == null) {
                    throw unprocessable("COMPOSITION constructor steps require an instance name");
                }
                namedCount = registerName(name, usedNames, namedCount);
                validateArgs(
                        step.params(),
                        memberIds.paramTypesByConstructorId().getOrDefault(step.constructorId(), List.of()),
                        named,
                        true,
                        memberIds,
                        "Invocation params");
                named.put(name, memberIds.classNameByConstructorId().get(step.constructorId()));
            } else if (step.invocationKind() == InvocationKind.METHOD) {
                boolean isStatic = isStaticMethod(step.methodId(), memberIds);
                String name = blankToNull(step.instanceName());
                String returnType = memberIds.methodReturnTypeById().get(step.methodId());
                if (!isStatic) {
                    if (name == null || !named.containsKey(name)) {
                        throw unprocessable(
                                "Method receiver instance '" + (name == null ? "" : name)
                                        + "' is not constructed earlier");
                    }
                }
                validateArgs(
                        step.params(),
                        memberIds.paramTypesByMethodId().getOrDefault(step.methodId(), List.of()),
                        named,
                        true,
                        memberIds,
                        "Invocation params");
                if (step.receiverConstructorId() != null) {
                    validateArgs(
                            step.receiverParams(),
                            memberIds.paramTypesByConstructorId()
                                    .getOrDefault(step.receiverConstructorId(), List.of()),
                            named,
                            true,
                            memberIds,
                            "Receiver params");
                }
                if (isStatic && name != null) {
                    String producedType = coreTypeName(returnType);
                    if (!memberIds.rubricClassNames().contains(producedType)) {
                        throw unprocessable("Only rubric-class method returns may be named");
                    }
                    namedCount = registerName(name, usedNames, namedCount);
                    named.put(name, producedType);
                }
            }
        }
        return named;
    }

    private void validateAssertions(TestcaseStructureDTO dto,
                                    List<InvocationStructureDTO> steps,
                                    ChallengeMemberIds memberIds,
                                    Map<String, String> namedInstances) {
        Map<UUID, InvocationStructureDTO> stepsById = new HashMap<>();
        for (InvocationStructureDTO step : steps) {
            if (step.id() != null) {
                stepsById.put(step.id(), step);
            }
        }
        Set<UUID> stepIds = stepsById.keySet();
        for (AssertionStructureDTO assertion : dto.assertions()) {
            if (assertion.invocationId() == null || !stepIds.contains(assertion.invocationId())) {
                throw unprocessable("Assertion must reference a step in this testcase");
            }
            if (assertion.assertionKind() == AssertionKind.FIELD_STATE) {
                if (assertion.fieldId() == null || !memberIds.fieldIds().contains(assertion.fieldId())) {
                    throw unprocessable("FIELD_STATE assertion requires a field in this challenge");
                }
            } else if (assertion.fieldId() != null) {
                throw unprocessable("Only FIELD_STATE assertions may reference a field");
            }
            InvocationStructureDTO step = stepsById.get(assertion.invocationId());
            validateAssertionKind(assertion.assertionKind(), step, memberIds);
            validateExpectedValue(dto.testcaseType(), assertion, namedInstances);
        }
    }

    private void validateAssertionKind(AssertionKind kind,
                                       InvocationStructureDTO step,
                                       ChallengeMemberIds memberIds) {
        if (step.invocationKind() == InvocationKind.CONSTRUCTOR) {
            if (kind == AssertionKind.STDOUT) {
                throw unprocessable("Constructor steps cannot assert stdout");
            }
            if (kind == AssertionKind.RETURN_VALUE) {
                throw unprocessable("Constructor steps cannot assert return value; use field state");
            }
            return;
        }
        String returnType = memberIds.methodReturnTypeById().get(step.methodId());
        boolean isVoid = returnType == null || "void".equalsIgnoreCase(returnType);
        if (isVoid && kind == AssertionKind.RETURN_VALUE) {
            throw unprocessable("Void methods cannot assert a return value");
        }
    }

    private void validateExpectedValue(TestcaseType testcaseType,
                                       AssertionStructureDTO assertion,
                                       Map<String, String> namedInstances) {
        String raw = assertion.expectedValue();
        if (raw == null || raw.isBlank()) {
            return;
        }
        JsonNode node = parseJsonNode(raw.trim(), "Expected value", false);
        if (assertion.assertionKind() == AssertionKind.EXCEPTION) {
            if (!node.isTextual() || node.asText().isBlank()) {
                throw unprocessable("Exception assertion expected value must be the exception type");
            }
            return;
        }
        if (node.isObject() && node.has(OBJECT_CHECK_KEY)) {
            validateObjectCheck(testcaseType, node, namedInstances);
            return;
        }
        if (assertion.assertionKind() == AssertionKind.FIELD_STATE
                && node.isObject()
                && node.has(INSTANCE_REF_KEY)) {
            if (testcaseType == TestcaseType.UNIT) {
                throw unprocessable("UNIT field-state cannot name an instance");
            }
            requireNamedInstance(node.get(INSTANCE_REF_KEY), namedInstances, "Field-state");
        }
    }

    private void validateObjectCheck(TestcaseType testcaseType,
                                     JsonNode node,
                                     Map<String, String> namedInstances) {
        JsonNode kindNode = node.get(OBJECT_CHECK_KEY);
        if (kindNode == null || !kindNode.isTextual()) {
            throw unprocessable("Object check $objectCheck must be a string");
        }
        String kind = kindNode.asText();
        if (OBJECT_CHECK_TYPE.equals(kind)) {
            throw unprocessable("Type-only object checks are not supported; use field state");
        }
        if (OBJECT_CHECK_FIELDS.equals(kind)) {
            throw unprocessable("Object field maps are not supported; use field state assertions");
        }
        if (OBJECT_CHECK_EQUALS.equals(kind)) {
            if (testcaseType != TestcaseType.COMPOSITION) {
                throw unprocessable("equals() object checks are Composition-only");
            }
            requireNamedInstance(node.get(INSTANCE_REF_KEY), namedInstances, "equals() object check");
            return;
        }
        throw unprocessable("Unknown object check: " + kind);
    }

    private void validateInvocation(InvocationStructureDTO invocation,
                                    ChallengeMemberIds memberIds,
                                    boolean unit) {
        if (invocation.invocationKind() == InvocationKind.CONSTRUCTOR) {
            validateConstructorRef(invocation.constructorId(), memberIds);
            if (invocation.methodId() != null) {
                throw unprocessable("CONSTRUCTOR invocation must not set methodId");
            }
        } else if (invocation.invocationKind() == InvocationKind.METHOD) {
            if (invocation.methodId() == null || !memberIds.methodIds().contains(invocation.methodId())) {
                throw unprocessable("METHOD invocation requires a method in this challenge");
            }
            if (!unit && invocation.receiverConstructorId() != null) {
                validateConstructorRef(invocation.receiverConstructorId(), memberIds);
            }
        } else {
            throw unprocessable("Unknown invocation kind");
        }
        if (invocation.dispatchClassId() != null && !memberIds.classIds().contains(invocation.dispatchClassId())) {
            throw unprocessable("Dispatch class does not belong to this challenge");
        }
        validateJsonArray(invocation.params(), "Invocation params");
        validateJsonArray(invocation.receiverParams(), "Receiver params");
    }

    private void validateArgs(String raw,
                              List<String> paramTypes,
                              Map<String, String> namedEarlier,
                              boolean allowInstanceRefs,
                              ChallengeMemberIds memberIds,
                              String label) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        JsonNode node = parseJsonNode(raw, label, true);
        rejectObjectArrayArgs(raw, paramTypes, memberIds, label);
        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);
            String paramType = i < paramTypes.size() ? paramTypes.get(i) : null;
            if (element != null && element.isObject() && element.has(INSTANCE_REF_KEY)) {
                if (!allowInstanceRefs) {
                    throw unprocessable("UNIT cannot pass named instances as arguments");
                }
                String name = requireNamedInstance(element.get(INSTANCE_REF_KEY), namedEarlier, label);
                if (paramType == null) {
                    throw unprocessable(label + " $instance does not match a rubric parameter");
                }
                String expectedType = coreTypeName(paramType);
                String actualType = namedEarlier.get(name);
                if (expectedType == null || actualType == null
                        || !instanceTypeMatchesParameter(expectedType, actualType, memberIds)) {
                    throw unprocessable("Named instance '" + name + "' type does not match parameter type");
                }
            } else if (paramType != null
                    && memberIds.rubricClassNames().contains(coreTypeName(paramType))
                    && !isArrayOrListType(paramType)) {
                throw unprocessable("Rubric-class arguments must be named instances");
            }
        }
    }

    private void rejectObjectArrayArgs(String raw,
                                       List<String> paramTypes,
                                       ChallengeMemberIds memberIds,
                                       String label) {
        for (String paramType : paramTypes) {
            if (isArrayOrListType(paramType)
                    && memberIds.rubricClassNames().contains(coreTypeName(paramType))) {
                throw unprocessable("Arrays or lists of named objects are not valid as one argument");
            }
        }
        if (raw == null || raw.isBlank()) {
            return;
        }
        JsonNode node = parseJsonNode(raw, label, true);
        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);
            if (element != null && element.isArray() && containsInstanceRef(element)) {
                throw unprocessable("Arrays or lists of named objects are not valid as one argument");
            }
        }
    }

    private int registerName(String name, Set<String> usedNames, int namedCount) {
        if (!usedNames.add(name)) {
            throw unprocessable("Duplicate instance name: " + name);
        }
        int next = namedCount + 1;
        if (next > MAX_NAMED_INSTANCES) {
            throw unprocessable("A testcase may have at most " + MAX_NAMED_INSTANCES + " named instances");
        }
        return next;
    }

    private String requireNamedInstance(JsonNode nameNode,
                                        Map<String, String> namedInstances,
                                        String label) {
        if (nameNode == null || !nameNode.isTextual() || nameNode.asText().isBlank()) {
            throw unprocessable(label + " $instance must be a non-blank name");
        }
        String name = nameNode.asText();
        if (!namedInstances.containsKey(name)) {
            throw unprocessable("Named instance '" + name + "' is not constructed earlier");
        }
        return name;
    }

    private boolean containsInstanceRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            return containsInstanceRef(objectMapper.readTree(raw));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean containsInstanceRef(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isObject()) {
            if (node.has(INSTANCE_REF_KEY)) {
                return true;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                if (containsInstanceRef(fields.next().getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsInstanceRef(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateLiteralNode(JsonNode node, String label) {
        if (node == null || node.isNull() || node.isNumber() || node.isTextual() || node.isBoolean()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                validateLiteralNode(child, label);
            }
            return;
        }
        throw unprocessable(label + " values must be literals");
    }

    private boolean hasRubricClassArgument(List<String> paramTypes, ChallengeMemberIds memberIds) {
        for (String paramType : paramTypes) {
            if (memberIds.rubricClassNames().contains(coreTypeName(paramType))) {
                return true;
            }
        }
        return false;
    }

    private List<String> paramTypesFor(InvocationStructureDTO step, ChallengeMemberIds memberIds) {
        if (step.invocationKind() == InvocationKind.CONSTRUCTOR) {
            return memberIds.paramTypesByConstructorId().getOrDefault(step.constructorId(), List.of());
        }
        return memberIds.paramTypesByMethodId().getOrDefault(step.methodId(), List.of());
    }

    private boolean isStaticMethod(UUID methodId, ChallengeMemberIds memberIds) {
        return Boolean.TRUE.equals(memberIds.methodStaticById().get(methodId));
    }

    private static String coreTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        String trimmed = typeName.trim();
        if (trimmed.endsWith("[]")) {
            return coreTypeName(trimmed.substring(0, trimmed.length() - 2));
        }
        int genericStart = trimmed.indexOf('<');
        int genericEnd = trimmed.lastIndexOf('>');
        if (genericStart > 0 && genericEnd > genericStart) {
            return coreTypeName(trimmed.substring(genericStart + 1, genericEnd));
        }
        int dot = trimmed.lastIndexOf('.');
        return dot < 0 ? trimmed : trimmed.substring(dot + 1);
    }

    private static boolean isArrayOrListType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        String trimmed = typeName.trim();
        if (trimmed.endsWith("[]")) {
            return true;
        }
        String raw = trimmed.contains("<") ? trimmed.substring(0, trimmed.indexOf('<')).trim() : trimmed;
        int dot = raw.lastIndexOf('.');
        String simple = dot < 0 ? raw : raw.substring(dot + 1);
        return simple.equals("List")
                || simple.equals("ArrayList")
                || simple.equals("LinkedList")
                || simple.equals("Collection")
                || simple.equals("Set")
                || simple.equals("HashSet");
    }

    private void validateJsonArray(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        parseJsonNode(raw, label, true);
    }

    private void validateConstructorRef(UUID constructorId, ChallengeMemberIds memberIds) {
        if (constructorId == null || !memberIds.constructorIds().contains(constructorId)) {
            throw unprocessable("Constructor does not belong to this challenge");
        }
    }

    private Testcase upsertTestcase(Challenge challenge, TestcaseStructureDTO dto) {
        Testcase testcase;
        boolean isNew;
        if (dto.id() != null) {
            testcase = testcaseRepository.findById(dto.id()).orElse(null);
            if (testcase == null) {
                testcase = new Testcase();
                testcase.setId(dto.id());
                testcase.setChallenge(challenge);
                isNew = true;
            } else if (!testcase.getChallenge().getId().equals(challenge.getId())) {
                throw unprocessable("Testcase does not belong to this challenge");
            } else {
                isNew = false;
            }
        } else {
            testcase = new Testcase();
            testcase.setId(UUID.randomUUID());
            testcase.setChallenge(challenge);
            isNew = true;
        }
        testcase.setName(dto.name().trim());
        testcase.setTestcaseType(dto.testcaseType());
        testcase.setOrderIndex(dto.orderIndex());
        testcase.setHidden(dto.hidden());
        if (isNew) {
            entityManager.persist(testcase);
        } else {
            testcase = testcaseRepository.save(testcase);
        }
        entityManager.flush();
        return testcase;
    }

    private Map<UUID, TestcaseInvocation> syncInvocations(Testcase testcase,
                                                          List<InvocationStructureDTO> steps,
                                                          ChallengeMemberIds memberIds) {
        List<TestcaseInvocation> existing = testcaseInvocationRepository
                .findByTestcase_IdIn(List.of(testcase.getId()));
        Map<UUID, TestcaseInvocation> existingById = existing.stream()
                .collect(Collectors.toMap(TestcaseInvocation::getId, row -> row, (left, right) -> left));

        if (steps == null || steps.isEmpty()) {
            testcaseInvocationRepository.deleteAll(existing);
            return Map.of();
        }

        Map<UUID, TestcaseInvocation> kept = new LinkedHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            InvocationStructureDTO dto = steps.get(i);
            TestcaseInvocation invocation = resolveInvocation(existing, existingById, dto.id(), steps.size());
            boolean isNew = invocation == null;
            if (isNew) {
                invocation = new TestcaseInvocation();
                invocation.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
                invocation.setTestcase(testcase);
            }
            kept.put(invocation.getId(), invocation);
            applyInvocation(invocation, dto, ORDER_INDEX_PARK + i, memberIds);
            if (isNew) {
                entityManager.persist(invocation);
            } else {
                testcaseInvocationRepository.save(invocation);
            }
        }
        entityManager.flush();

        for (TestcaseInvocation extra : existing) {
            if (!kept.containsKey(extra.getId())) {
                testcaseInvocationRepository.delete(extra);
            }
        }
        entityManager.flush();

        int orderIndex = 0;
        for (TestcaseInvocation invocation : kept.values()) {
            invocation.setOrderIndex(orderIndex++);
            testcaseInvocationRepository.save(invocation);
        }
        entityManager.flush();
        return kept;
    }

    private TestcaseInvocation resolveInvocation(List<TestcaseInvocation> existing,
                                                 Map<UUID, TestcaseInvocation> existingById,
                                                 UUID dtoId,
                                                 int stepCount) {
        if (dtoId != null) {
            return existingById.get(dtoId);
        }
        if (stepCount == 1 && existing.size() == 1) {
            return existing.get(0);
        }
        return null;
    }

    private void applyInvocation(TestcaseInvocation invocation,
                                 InvocationStructureDTO dto,
                                 int orderIndex,
                                 ChallengeMemberIds memberIds) {
        invocation.setInvocationKind(dto.invocationKind());
        invocation.setParams(normalizeJsonArray(dto.params()));
        invocation.setReceiverParams(normalizeJsonArray(dto.receiverParams()));
        invocation.setOrderIndex(orderIndex);
        invocation.setInstanceName(blankToNull(dto.instanceName()));
        if (dto.invocationKind() == InvocationKind.CONSTRUCTOR) {
            invocation.setConstructor(requireConstructor(dto.constructorId(), memberIds));
            invocation.setMethod(null);
            invocation.setReceiverConstructor(null);
            invocation.setDispatchClass(null);
        } else {
            invocation.setMethod(requireMethod(dto.methodId(), memberIds));
            invocation.setConstructor(null);
            invocation.setDispatchClass(requireClass(dto.dispatchClassId(), memberIds));
            if (dto.receiverConstructorId() != null) {
                invocation.setReceiverConstructor(requireConstructor(dto.receiverConstructorId(), memberIds));
            } else {
                invocation.setReceiverConstructor(null);
            }
        }
    }

    private void syncInstances(Testcase testcase,
                               List<InstanceStructureDTO> dtos,
                               ChallengeMemberIds memberIds) {
        // testcase_instance was dropped in the UNIT/COMPOSITION wipe.
    }

    private void syncAssertions(Testcase testcase,
                                List<AssertionStructureDTO> dtos,
                                ChallengeMemberIds memberIds,
                                Map<UUID, TestcaseInvocation> invocationsById) {
        List<TestcaseAssertion> existing = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(List.of(testcase.getId()));
        Map<UUID, TestcaseAssertion> existingById = existing.stream()
                .collect(Collectors.toMap(TestcaseAssertion::getId, row -> row, (left, right) -> left));

        if (dtos == null || dtos.isEmpty()) {
            testcaseAssertionRepository.deleteAll(existing);
            return;
        }

        Set<UUID> keptIds = new HashSet<>();

        for (AssertionStructureDTO dto : dtos) {
            UUID assertionId = dto.id() != null ? dto.id() : UUID.randomUUID();
            keptIds.add(assertionId);

            TestcaseAssertion assertion = existingById.get(assertionId);
            boolean isNew = assertion == null;
            if (isNew) {
                assertion = new TestcaseAssertion();
                assertion.setId(assertionId);
                assertion.setTestcase(testcase);
            }

            assertion.setAssertionKind(dto.assertionKind());
            assertion.setExpectedValue(normalizeExpectedValue(dto.expectedValue()));
            assertion.setComparisonMode(
                    dto.comparisonMode() != null ? dto.comparisonMode() : ComparisonMode.EXACT);
            assertion.setOrderIndex(dto.orderIndex());
            if (dto.assertionKind() == AssertionKind.FIELD_STATE) {
                assertion.setField(requireField(dto.fieldId(), memberIds));
            } else {
                assertion.setField(null);
            }
            if (dto.invocationId() != null && invocationsById.containsKey(dto.invocationId())) {
                assertion.setInvocation(invocationsById.get(dto.invocationId()));
            } else {
                assertion.setInvocation(null);
            }

            if (isNew) {
                entityManager.persist(assertion);
            } else {
                testcaseAssertionRepository.save(assertion);
            }
        }

        for (TestcaseAssertion row : existing) {
            if (!keptIds.contains(row.getId())) {
                testcaseAssertionRepository.delete(row);
            }
        }
    }

    private void deleteTestcaseGraph(UUID testcaseId) {
        deleteAssertionsForTestcase(testcaseId);
        deleteInvocationsForTestcase(testcaseId);
    }

    private void deleteAssertionsForTestcase(UUID testcaseId) {
        List<TestcaseAssertion> assertions = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(List.of(testcaseId));
        testcaseAssertionRepository.deleteAll(assertions);
    }

    private void deleteInvocationsForTestcase(UUID testcaseId) {
        List<TestcaseInvocation> invocations = testcaseInvocationRepository.findByTestcase_IdIn(List.of(testcaseId));
        testcaseInvocationRepository.deleteAll(invocations);
    }

    private ChallengeMemberIds loadChallengeMemberIds(UUID challengeId) {
        Map<UUID, ChallengeMemberIds> grouped = loadChallengeMemberIdsGrouped(List.of(challengeId));
        ChallengeMemberIds memberIds = grouped.get(challengeId);
        return memberIds != null ? memberIds : emptyChallengeMemberIds();
    }

    /**
     * Batch-load OT membership sets for many challenges (clone path — one query set, not N).
     */
    private Map<UUID, ChallengeMemberIds> loadChallengeMemberIdsGrouped(Collection<UUID> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = challengeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Challenge> challenges = challengeRepository.findAllById(ids);
        Map<UUID, ChallengeMemberIds> result = new HashMap<>();
        for (UUID id : ids) {
            result.put(id, emptyChallengeMemberIds());
        }
        if (challenges.isEmpty()) {
            return result;
        }
        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        Map<UUID, List<ClassEntity>> classesByChallenge = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        // classEntity on Constructor/Method/Field is often an uninitialized proxy (OSIV off).
        // Resolve challenge via class id — do not call getChallenge() on that proxy.
        Map<UUID, UUID> challengeIdByClassId = new HashMap<>();
        for (ClassEntity cls : classes) {
            challengeIdByClassId.put(cls.getId(), cls.getChallenge().getId());
        }

        Map<UUID, Set<UUID>> heritageChildrenByParentId = new HashMap<>();
        if (!classes.isEmpty()) {
            for (ClassRelation relation : classRelationRepository.findByClassEntityInWithEndpoints(classes)) {
                if (relation.getRelationType() == null
                        || !isHeritageRelationType(relation.getRelationType().getName())) {
                    continue;
                }
                UUID childId = relation.getClassEntity().getId();
                UUID parentId = relation.getTargetClassEntity().getId();
                heritageChildrenByParentId
                        .computeIfAbsent(parentId, ignored -> new HashSet<>())
                        .add(childId);
            }
        }

        List<Constructor> allConstructors = classes.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> allMethods = classes.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Field> allFields = classes.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Parameter> constructorParams = allConstructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);
        List<Parameter> methodParams = allMethods.isEmpty() ? List.of()
                : parameterRepository.findByMethodIn(allMethods);
        Map<UUID, List<String>> paramTypesByConstructorId = RubricParameterMaps.byConstructor(constructorParams);
        Map<UUID, List<String>> paramTypesByMethodId = RubricParameterMaps.byMethod(methodParams);

        Map<UUID, List<Constructor>> ctorsByChallenge = new HashMap<>();
        Map<UUID, List<Method>> methodsByChallenge = new HashMap<>();
        Map<UUID, List<Field>> fieldsByChallenge = new HashMap<>();
        for (Constructor ctor : allConstructors) {
            UUID challengeId = challengeIdByClassId.get(ctor.getClassEntity().getId());
            if (challengeId != null) {
                ctorsByChallenge.computeIfAbsent(challengeId, ignored -> new ArrayList<>()).add(ctor);
            }
        }
        for (Method method : allMethods) {
            UUID challengeId = challengeIdByClassId.get(method.getClassEntity().getId());
            if (challengeId != null) {
                methodsByChallenge.computeIfAbsent(challengeId, ignored -> new ArrayList<>()).add(method);
            }
        }
        for (Field field : allFields) {
            UUID challengeId = challengeIdByClassId.get(field.getClassEntity().getId());
            if (challengeId != null) {
                fieldsByChallenge.computeIfAbsent(challengeId, ignored -> new ArrayList<>()).add(field);
            }
        }

        for (Challenge challenge : challenges) {
            UUID challengeId = challenge.getId();
            List<ClassEntity> challengeClasses = classesByChallenge.getOrDefault(challengeId, List.of());
            Set<UUID> classIds = new HashSet<>();
            Set<String> rubricClassNames = new HashSet<>();
            Map<UUID, String> classNameByClassId = new HashMap<>();
            Map<String, UUID> classIdByName = new HashMap<>();
            for (ClassEntity cls : challengeClasses) {
                classIds.add(cls.getId());
                classNameByClassId.put(cls.getId(), cls.getName());
                if (cls.getName() != null && !cls.getName().isBlank()) {
                    rubricClassNames.add(cls.getName());
                    classIdByName.put(cls.getName(), cls.getId());
                }
            }

            Set<UUID> constructorIds = new HashSet<>();
            Set<UUID> methodIds = new HashSet<>();
            Set<UUID> fieldIds = new HashSet<>();
            Map<UUID, String> classNameByConstructorId = new HashMap<>();
            Map<UUID, UUID> classIdByMethodId = new HashMap<>();
            Map<UUID, Boolean> methodStaticById = new HashMap<>();
            Map<UUID, String> methodReturnTypeById = new HashMap<>();
            Set<UUID> noArgConstructorClassIds = new HashSet<>();
            Map<UUID, List<String>> challengeCtorParams = new HashMap<>();
            Map<UUID, List<String>> challengeMethodParams = new HashMap<>();

            for (Constructor ctor : ctorsByChallenge.getOrDefault(challengeId, List.of())) {
                constructorIds.add(ctor.getId());
                classNameByConstructorId.put(
                        ctor.getId(),
                        classNameByClassId.get(ctor.getClassEntity().getId()));
                List<String> types = paramTypesByConstructorId.getOrDefault(ctor.getId(), List.of());
                challengeCtorParams.put(ctor.getId(), types);
                if (types.isEmpty()) {
                    noArgConstructorClassIds.add(ctor.getClassEntity().getId());
                }
            }
            for (Method method : methodsByChallenge.getOrDefault(challengeId, List.of())) {
                methodIds.add(method.getId());
                classIdByMethodId.put(method.getId(), method.getClassEntity().getId());
                if (method.getMethodDeclaration() != null) {
                    methodStaticById.put(method.getId(), method.getMethodDeclaration().isStatic());
                    methodReturnTypeById.put(method.getId(), method.getMethodDeclaration().getReturnType());
                }
                challengeMethodParams.put(
                        method.getId(),
                        paramTypesByMethodId.getOrDefault(method.getId(), List.of()));
            }
            for (Field field : fieldsByChallenge.getOrDefault(challengeId, List.of())) {
                fieldIds.add(field.getId());
            }

            // Heritage map is lab-wide parent→children; ChallengeMemberIds only walks within
            // ids present in this challenge's classIdByName, so shared map is fine.
            result.put(challengeId, new ChallengeMemberIds(
                    constructorIds,
                    methodIds,
                    fieldIds,
                    classIds,
                    rubricClassNames,
                    classIdByName,
                    heritageChildrenByParentId,
                    classNameByConstructorId,
                    classIdByMethodId,
                    methodStaticById,
                    methodReturnTypeById,
                    noArgConstructorClassIds,
                    challengeCtorParams,
                    challengeMethodParams));
        }
        return result;
    }

    private static ChallengeMemberIds emptyChallengeMemberIds() {
        return new ChallengeMemberIds(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of());
    }

    private static boolean isHeritageRelationType(String relationTypeName) {
        if (relationTypeName == null || relationTypeName.isBlank()) {
            return false;
        }
        String normalized = relationTypeName.trim().toLowerCase();
        return normalized.contains("realiz")
                || normalized.contains("implement")
                || normalized.contains("inherit")
                || normalized.contains("extend")
                || normalized.contains("general");
    }

    private static boolean instanceTypeMatchesParameter(String paramType,
                                                        String instanceClassName,
                                                        ChallengeMemberIds memberIds) {
        if (paramType.equals(instanceClassName)) {
            return true;
        }
        UUID parentId = memberIds.classIdByName().get(paramType);
        UUID childId = memberIds.classIdByName().get(instanceClassName);
        if (parentId == null || childId == null) {
            return false;
        }
        return memberIds.isHeritageDescendant(parentId, childId);
    }

    private Constructor requireConstructor(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.constructorIds().contains(id)) {
            throw unprocessable("Invalid constructor for this challenge");
        }
        // Membership already proven from the challenge graph — avoid a Neon SELECT per FK.
        return entityManager.getReference(Constructor.class, id);
    }

    private Method requireMethod(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.methodIds().contains(id)) {
            throw unprocessable("Invalid method for this challenge");
        }
        return entityManager.getReference(Method.class, id);
    }

    private Field requireField(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.fieldIds().contains(id)) {
            throw unprocessable("Invalid field for this challenge");
        }
        return entityManager.getReference(Field.class, id);
    }

    private ClassEntity requireClass(UUID id, ChallengeMemberIds memberIds) {
        if (id == null) {
            return null;
        }
        if (!memberIds.classIds().contains(id)) {
            throw unprocessable("Dispatch class does not belong to this challenge");
        }
        return entityManager.getReference(ClassEntity.class, id);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private String normalizeExpectedValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "null";
        }
        String trimmed = raw.trim();
        parseJsonNode(trimmed, "Expected value", false);
        return trimmed;
    }

    private String normalizeJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        String trimmed = raw.trim();
        parseJsonNode(trimmed, "Params", true);
        return trimmed;
    }

    private JsonNode parseJsonNode(String raw, String label, boolean requireArray) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (requireArray && !node.isArray()) {
                throw unprocessable(label + " must be a JSON array");
            }
            return node;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unprocessable(label + " must be valid JSON");
        }
    }

    private record ChallengeMemberIds(
            Set<UUID> constructorIds,
            Set<UUID> methodIds,
            Set<UUID> fieldIds,
            Set<UUID> classIds,
            Set<String> rubricClassNames,
            Map<String, UUID> classIdByName,
            Map<UUID, Set<UUID>> heritageChildrenByParentId,
            Map<UUID, String> classNameByConstructorId,
            Map<UUID, UUID> classIdByMethodId,
            Map<UUID, Boolean> methodStaticById,
            Map<UUID, String> methodReturnTypeById,
            Set<UUID> noArgConstructorClassIds,
            Map<UUID, List<String>> paramTypesByConstructorId,
            Map<UUID, List<String>> paramTypesByMethodId) {

        boolean isHeritageDescendant(UUID ancestorClassId, UUID descendantClassId) {
            if (ancestorClassId == null || descendantClassId == null) {
                return false;
            }
            Set<UUID> visited = new HashSet<>();
            Deque<UUID> stack = new ArrayDeque<>(
                    heritageChildrenByParentId.getOrDefault(ancestorClassId, Set.of()));
            while (!stack.isEmpty()) {
                UUID id = stack.pop();
                if (id.equals(descendantClassId)) {
                    return true;
                }
                if (!visited.add(id)) {
                    continue;
                }
                for (UUID child : heritageChildrenByParentId.getOrDefault(id, Set.of())) {
                    stack.push(child);
                }
            }
            return false;
        }
    }
}
