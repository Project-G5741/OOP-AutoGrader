package com.eiu.capstone.backend.service;

import java.util.Comparator;
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
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInstance;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInstanceRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TestcaseRubricService {

    public enum RubricMemberKind {
        METHOD, CONSTRUCTOR, FIELD, CLASS
    }

    enum GuardrailMode {
        SAVE, PREVIEW
    }

    static final int MAX_STEPS = 20;
    static final int MAX_NAMED_INSTANCES = 10;
    private static final int ORDER_INDEX_PARK = MAX_STEPS;
    private static final String INSTANCE_REF_KEY = "$instance";

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ConstructorRepository constructorRepository;
    private final MethodRepository methodRepository;
    private final FieldRepository fieldRepository;
    private final ParameterRepository parameterRepository;
    private final TestcaseRepository testcaseRepository;
    private final TestcaseInvocationRepository testcaseInvocationRepository;
    private final TestcaseInstanceRepository testcaseInstanceRepository;
    private final TestcaseAssertionRepository testcaseAssertionRepository;
    private final RubricCacheInvalidationSupport rubricCacheInvalidationSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityManager entityManager;

    public TestcaseRubricService(ChallengeRepository challengeRepository,
                                   ClassEntityRepository classEntityRepository,
                                   ConstructorRepository constructorRepository,
                                   MethodRepository methodRepository,
                                   FieldRepository fieldRepository,
                                   ParameterRepository parameterRepository,
                                   TestcaseRepository testcaseRepository,
                                   TestcaseInvocationRepository testcaseInvocationRepository,
                                   TestcaseInstanceRepository testcaseInstanceRepository,
                                   TestcaseAssertionRepository testcaseAssertionRepository,
                                   RubricCacheInvalidationSupport rubricCacheInvalidationSupport,
                                   EntityManager entityManager) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.constructorRepository = constructorRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.parameterRepository = parameterRepository;
        this.testcaseRepository = testcaseRepository;
        this.testcaseInvocationRepository = testcaseInvocationRepository;
        this.testcaseInstanceRepository = testcaseInstanceRepository;
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

    public void validatePayload(UUID challengeId, TestcaseStructureDTO dto) {
        validateTestcaseDto(dto, loadChallengeMemberIds(challengeId), GuardrailMode.PREVIEW);
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
                                                       List<TestcaseStructureDTO> payloads) {
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
            validateTestcaseDto(dto, memberIds, GuardrailMode.SAVE);
            Testcase testcase = upsertTestcase(challenge, dto);
            Map<UUID, TestcaseInvocation> invocations = syncInvocations(
                    testcase, resolvedInvocations(dto), memberIds);
            syncInstances(testcase, dto.instances(), memberIds);
            syncAssertions(testcase, dto.assertions(), memberIds, invocations);
        }

        rubricCacheInvalidationSupport.invalidateLab(labId);
        return new ChallengeTestcasesResponse(labId, challengeId, loadDtosForChallenge(challenge.getId()));
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
            testcaseInstanceRepository.findByTestcase_IdIn(testcaseIds).stream()
                    .filter(inst -> inst.getConstructor() != null && memberId.equals(inst.getConstructor().getId()))
                    .forEach(inst -> referenced.add(inst.getTestcase().getId()));
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

        Map<UUID, List<TestcaseInstance>> instancesByTestcaseId = testcaseInstanceRepository
                .findByTestcase_IdIn(testcaseIds).stream()
                .collect(Collectors.groupingBy(inst -> inst.getTestcase().getId()));

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
                testcase.getComparisonMethod(),
                testcase.getWeight(),
                testcase.getOrderIndex(),
                testcase.isHidden(),
                singular,
                instanceDtos,
                assertionDtos,
                invocationDtos,
                testcase.getOopPrincipleTag() != null ? testcase.getOopPrincipleTag() : OopPrincipleTag.Unit);
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

    private void validateTestcaseDto(TestcaseStructureDTO dto,
                                     ChallengeMemberIds memberIds,
                                     GuardrailMode mode) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw unprocessable("Testcase name is required");
        }
        if (dto.testcaseType() == null) {
            throw unprocessable("Testcase type is required");
        }
        List<InvocationStructureDTO> steps = resolvedInvocations(dto);
        if (dto.testcaseType() == TestcaseType.SINGLE_INVOCATION) {
            if (steps.isEmpty()) {
                throw unprocessable("SINGLE_INVOCATION requires an invocation");
            }
            if (dto.instances() != null && !dto.instances().isEmpty()) {
                throw unprocessable("SINGLE_INVOCATION must not have instances");
            }
            validateScenarioSteps(steps, memberIds);
            if (mode == GuardrailMode.SAVE
                    && resolveTag(dto) == OopPrincipleTag.Polymorphism
                    && steps.stream().noneMatch(TestcaseRubricService::isMethodDispatchStep)) {
                throw unprocessable(
                        "Polymorphism testcases require at least one call with a dispatch type");
            }
        } else if (dto.testcaseType() == TestcaseType.COMPARISON) {
            if (dto.comparisonMethod() == null) {
                throw unprocessable("COMPARISON requires comparisonMethod");
            }
            if (dto.instances() == null || dto.instances().size() != 2) {
                throw unprocessable("COMPARISON requires exactly two instances");
            }
            for (InstanceStructureDTO inst : dto.instances()) {
                validateConstructorRef(inst.constructorId(), memberIds);
            }
            if (dto.invocation() != null || !steps.isEmpty()) {
                throw unprocessable("COMPARISON must not have an invocation row");
            }
            boolean hasComparisonAssertion = dto.assertions() != null && dto.assertions().stream()
                    .anyMatch(a -> a.assertionKind() == AssertionKind.COMPARISON_RESULT);
            if (!hasComparisonAssertion) {
                throw unprocessable("COMPARISON requires a COMPARISON_RESULT assertion");
            }
        }
        if (dto.assertions() == null || dto.assertions().isEmpty()) {
            throw unprocessable("At least one assertion is required");
        }
        Set<UUID> stepIds = steps.stream()
                .map(InvocationStructureDTO::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (AssertionStructureDTO assertion : dto.assertions()) {
            if (dto.testcaseType() == TestcaseType.SINGLE_INVOCATION
                    && assertion.assertionKind() == AssertionKind.COMPARISON_RESULT) {
                throw unprocessable("COMPARISON_RESULT is only valid for COMPARISON testcases");
            }
            if (dto.testcaseType() == TestcaseType.SINGLE_INVOCATION
                    && (assertion.invocationId() == null || !stepIds.contains(assertion.invocationId()))) {
                throw unprocessable("Assertion must reference a step in this testcase");
            }
            if (assertion.assertionKind() == AssertionKind.FIELD_STATE) {
                if (assertion.fieldId() == null || !memberIds.fieldIds().contains(assertion.fieldId())) {
                    throw unprocessable("FIELD_STATE assertion requires a field in this challenge");
                }
            } else if (assertion.fieldId() != null) {
                throw unprocessable("Only FIELD_STATE assertions may reference a field");
            }
        }
    }

    private void validateScenarioSteps(List<InvocationStructureDTO> steps, ChallengeMemberIds memberIds) {
        if (steps.size() > MAX_STEPS) {
            throw unprocessable("A testcase may have at most " + MAX_STEPS + " steps");
        }
        Map<String, String> constructed = new LinkedHashMap<>();
        Set<String> constructNames = new HashSet<>();
        int namedCount = 0;
        for (InvocationStructureDTO step : steps) {
            validateInvocation(step, memberIds);
            if (step.dispatchClassId() != null && !memberIds.classIds().contains(step.dispatchClassId())) {
                throw unprocessable("Dispatch class does not belong to this challenge");
            }
            if (step.invocationKind() == InvocationKind.CONSTRUCTOR) {
                String name = blankToNull(step.instanceName());
                if (name != null) {
                    if (!constructNames.add(name)) {
                        throw unprocessable("Duplicate instance name: " + name);
                    }
                    namedCount++;
                    if (namedCount > MAX_NAMED_INSTANCES) {
                        throw unprocessable(
                                "A testcase may have at most " + MAX_NAMED_INSTANCES + " named instances");
                    }
                }
                validateInstanceRefs(
                        step.params(),
                        memberIds.paramTypesByConstructorId().getOrDefault(step.constructorId(), List.of()),
                        constructed,
                        "Invocation params");
                if (name != null) {
                    constructed.put(name, memberIds.classNameByConstructorId().get(step.constructorId()));
                }
            } else if (step.invocationKind() == InvocationKind.METHOD) {
                String receiverName = blankToNull(step.instanceName());
                if (receiverName != null && !constructed.containsKey(receiverName)) {
                    throw unprocessable(
                            "Method receiver instance '" + receiverName + "' is not constructed earlier");
                }
                validateInstanceRefs(
                        step.params(),
                        memberIds.paramTypesByMethodId().getOrDefault(step.methodId(), List.of()),
                        constructed,
                        "Invocation params");
                if (step.receiverConstructorId() != null) {
                    validateInstanceRefs(
                            step.receiverParams(),
                            memberIds.paramTypesByConstructorId()
                                    .getOrDefault(step.receiverConstructorId(), List.of()),
                            constructed,
                            "Receiver params");
                }
            }
        }
    }

    private void validateInvocation(InvocationStructureDTO invocation, ChallengeMemberIds memberIds) {
        if (invocation.invocationKind() == InvocationKind.CONSTRUCTOR) {
            validateConstructorRef(invocation.constructorId(), memberIds);
            if (invocation.methodId() != null) {
                throw unprocessable("CONSTRUCTOR invocation must not set methodId");
            }
        } else if (invocation.invocationKind() == InvocationKind.METHOD) {
            if (invocation.methodId() == null || !memberIds.methodIds().contains(invocation.methodId())) {
                throw unprocessable("METHOD invocation requires a method in this challenge");
            }
            if (invocation.receiverConstructorId() != null) {
                validateConstructorRef(invocation.receiverConstructorId(), memberIds);
            }
        } else {
            throw unprocessable("Unknown invocation kind");
        }
        validateJsonArray(invocation.params(), "Invocation params");
        validateJsonArray(invocation.receiverParams(), "Receiver params");
    }

    private void validateInstanceRefs(String raw,
                                      List<String> paramTypes,
                                      Map<String, String> constructedEarlier,
                                      String label) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        JsonNode node = parseJsonNode(raw, label, true);
        for (int i = 0; i < node.size(); i++) {
            JsonNode element = node.get(i);
            if (element == null || !element.isObject() || !element.has(INSTANCE_REF_KEY)) {
                continue;
            }
            JsonNode nameNode = element.get(INSTANCE_REF_KEY);
            if (nameNode == null || !nameNode.isTextual() || nameNode.asText().isBlank()) {
                throw unprocessable(label + " $instance must be a non-blank name");
            }
            String name = nameNode.asText();
            if (!constructedEarlier.containsKey(name)) {
                throw unprocessable("Named instance '" + name + "' is not constructed earlier");
            }
            if (i >= paramTypes.size()) {
                throw unprocessable(label + " $instance does not match a rubric parameter");
            }
            String expectedType = paramTypes.get(i);
            String actualType = constructedEarlier.get(name);
            if (expectedType == null || actualType == null || !expectedType.equals(actualType)) {
                throw unprocessable("Named instance '" + name + "' type does not match parameter type");
            }
        }
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
        testcase.setComparisonMethod(dto.comparisonMethod());
        testcase.setOopPrincipleTag(resolveTag(dto));
        testcase.setWeight(Math.max(1, dto.weight()));
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

    private static OopPrincipleTag resolveTag(TestcaseStructureDTO dto) {
        return dto.oopPrincipleTag() != null ? dto.oopPrincipleTag() : OopPrincipleTag.Unit;
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
        List<TestcaseInstance> existing = testcaseInstanceRepository.findByTestcase_IdIn(List.of(testcase.getId()));
        testcaseInstanceRepository.deleteAll(existing);
        if (dtos == null) {
            return;
        }
        for (InstanceStructureDTO dto : dtos) {
            TestcaseInstance instance = new TestcaseInstance();
            instance.setTestcase(testcase);
            instance.setLabel(dto.label());
            instance.setConstructor(requireConstructor(dto.constructorId(), memberIds));
            instance.setParams(normalizeJsonArray(dto.params()));
            testcaseInstanceRepository.save(instance);
        }
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
        List<TestcaseInstance> instances = testcaseInstanceRepository.findByTestcase_IdIn(List.of(testcaseId));
        testcaseInstanceRepository.deleteAll(instances);
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
        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challengeId);
        Set<UUID> classIds = new HashSet<>();
        Map<UUID, String> classNameByClassId = new HashMap<>();
        for (ClassEntity cls : classes) {
            classIds.add(cls.getId());
            classNameByClassId.put(cls.getId(), cls.getName());
        }
        Set<UUID> constructorIds = new HashSet<>();
        Set<UUID> methodIds = new HashSet<>();
        Set<UUID> fieldIds = new HashSet<>();
        Map<UUID, String> classNameByConstructorId = new HashMap<>();
        List<Constructor> constructors = List.of();
        List<Method> methods = List.of();
        if (!classes.isEmpty()) {
            constructors = constructorRepository.findByClassEntityInWithDeclaration(classes);
            for (Constructor ctor : constructors) {
                constructorIds.add(ctor.getId());
                classNameByConstructorId.put(
                        ctor.getId(),
                        classNameByClassId.get(ctor.getClassEntity().getId()));
            }
            methods = methodRepository.findByClassEntityInWithDeclaration(classes);
            for (Method method : methods) {
                methodIds.add(method.getId());
            }
            for (Field field : fieldRepository.findByClassEntityInWithDeclaration(classes)) {
                fieldIds.add(field.getId());
            }
        }
        List<Parameter> constructorParams = constructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(constructors);
        List<Parameter> methodParams = methods.isEmpty() ? List.of()
                : parameterRepository.findByMethodIn(methods);
        return new ChallengeMemberIds(
                constructorIds,
                methodIds,
                fieldIds,
                classIds,
                classNameByConstructorId,
                RubricParameterMaps.byConstructor(constructorParams),
                RubricParameterMaps.byMethod(methodParams));
    }

    private Constructor requireConstructor(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.constructorIds().contains(id)) {
            throw unprocessable("Invalid constructor for this challenge");
        }
        return constructorRepository.findById(id)
                .orElseThrow(() -> unprocessable("Constructor not found"));
    }

    private Method requireMethod(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.methodIds().contains(id)) {
            throw unprocessable("Invalid method for this challenge");
        }
        return methodRepository.findById(id)
                .orElseThrow(() -> unprocessable("Method not found"));
    }

    private Field requireField(UUID id, ChallengeMemberIds memberIds) {
        if (id == null || !memberIds.fieldIds().contains(id)) {
            throw unprocessable("Invalid field for this challenge");
        }
        return fieldRepository.findById(id)
                .orElseThrow(() -> unprocessable("Field not found"));
    }

    private ClassEntity requireClass(UUID id, ChallengeMemberIds memberIds) {
        if (id == null) {
            return null;
        }
        if (!memberIds.classIds().contains(id)) {
            throw unprocessable("Dispatch class does not belong to this challenge");
        }
        return classEntityRepository.findById(id)
                .orElseThrow(() -> unprocessable("Dispatch class not found"));
    }

    private static boolean isMethodDispatchStep(InvocationStructureDTO step) {
        return step.invocationKind() == InvocationKind.METHOD && step.dispatchClassId() != null;
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
            Map<UUID, String> classNameByConstructorId,
            Map<UUID, List<String>> paramTypesByConstructorId,
            Map<UUID, List<String>> paramTypesByMethodId) {}
}
