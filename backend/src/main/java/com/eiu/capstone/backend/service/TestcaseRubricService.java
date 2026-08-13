package com.eiu.capstone.backend.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
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
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInstanceRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TestcaseRubricService {

    public enum RubricMemberKind {
        METHOD, CONSTRUCTOR, FIELD
    }

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ConstructorRepository constructorRepository;
    private final MethodRepository methodRepository;
    private final FieldRepository fieldRepository;
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
        validateTestcaseDto(dto, loadChallengeMemberIds(challengeId));
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
            validateTestcaseDto(dto, memberIds);
            Testcase testcase = upsertTestcase(challenge, dto);
            syncInvocation(testcase, dto.invocation(), memberIds);
            syncInstances(testcase, dto.instances(), memberIds);
            syncAssertions(testcase, dto.assertions(), memberIds);
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

        Map<UUID, TestcaseInvocation> invocationByTestcaseId = testcaseInvocationRepository
                .findByTestcase_IdIn(testcaseIds).stream()
                .collect(Collectors.toMap(inv -> inv.getTestcase().getId(), inv -> inv, (a, b) -> a));

        Map<UUID, List<TestcaseInstance>> instancesByTestcaseId = testcaseInstanceRepository
                .findByTestcase_IdIn(testcaseIds).stream()
                .collect(Collectors.groupingBy(inst -> inst.getTestcase().getId()));

        Map<UUID, List<TestcaseAssertion>> assertionsByTestcaseId = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds).stream()
                .collect(Collectors.groupingBy(a -> a.getTestcase().getId()));

        return testcases.stream()
                .map(tc -> toDto(
                        tc,
                        invocationByTestcaseId.get(tc.getId()),
                        instancesByTestcaseId.getOrDefault(tc.getId(), List.of()),
                        assertionsByTestcaseId.getOrDefault(tc.getId(), List.of())))
                .toList();
    }

    private TestcaseStructureDTO toDto(Testcase testcase,
                                       TestcaseInvocation invocation,
                                       List<TestcaseInstance> instances,
                                       List<TestcaseAssertion> assertions) {
        InvocationStructureDTO invocationDto = null;
        if (invocation != null) {
            invocationDto = new InvocationStructureDTO(
                    invocation.getId(),
                    invocation.getInvocationKind(),
                    invocation.getConstructor() != null ? invocation.getConstructor().getId() : null,
                    invocation.getMethod() != null ? invocation.getMethod().getId() : null,
                    invocation.getParams(),
                    invocation.getReceiverConstructor() != null
                            ? invocation.getReceiverConstructor().getId() : null,
                    invocation.getReceiverParams());
        }

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
                invocationDto,
                instanceDtos,
                assertionDtos);
    }

    private void validateTestcaseDto(TestcaseStructureDTO dto, ChallengeMemberIds memberIds) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw unprocessable("Testcase name is required");
        }
        if (dto.testcaseType() == null) {
            throw unprocessable("Testcase type is required");
        }
        if (dto.testcaseType() == TestcaseType.SINGLE_INVOCATION) {
            if (dto.invocation() == null) {
                throw unprocessable("SINGLE_INVOCATION requires an invocation");
            }
            validateInvocation(dto.invocation(), memberIds);
            if (dto.instances() != null && !dto.instances().isEmpty()) {
                throw unprocessable("SINGLE_INVOCATION must not have instances");
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
            if (dto.invocation() != null) {
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
        for (AssertionStructureDTO assertion : dto.assertions()) {
            if (dto.testcaseType() == TestcaseType.SINGLE_INVOCATION
                    && assertion.assertionKind() == AssertionKind.COMPARISON_RESULT) {
                throw unprocessable("COMPARISON_RESULT is only valid for COMPARISON testcases");
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

    private void syncInvocation(Testcase testcase,
                                InvocationStructureDTO dto,
                                ChallengeMemberIds memberIds) {
        List<TestcaseInvocation> existing = testcaseInvocationRepository
                .findByTestcase_IdIn(List.of(testcase.getId()));
        if (dto == null) {
            testcaseInvocationRepository.deleteAll(existing);
            return;
        }

        TestcaseInvocation invocation = resolveInvocation(existing, dto.id());
        boolean isNew = invocation == null;
        if (isNew) {
            invocation = new TestcaseInvocation();
            invocation.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
            invocation.setTestcase(testcase);
        }

        invocation.setInvocationKind(dto.invocationKind());
        invocation.setParams(normalizeJsonArray(dto.params()));
        invocation.setReceiverParams(normalizeJsonArray(dto.receiverParams()));
        if (dto.invocationKind() == InvocationKind.CONSTRUCTOR) {
            invocation.setConstructor(requireConstructor(dto.constructorId(), memberIds));
            invocation.setMethod(null);
            invocation.setReceiverConstructor(null);
        } else {
            invocation.setMethod(requireMethod(dto.methodId(), memberIds));
            invocation.setConstructor(null);
            if (dto.receiverConstructorId() != null) {
                invocation.setReceiverConstructor(requireConstructor(dto.receiverConstructorId(), memberIds));
            } else {
                invocation.setReceiverConstructor(null);
            }
        }

        if (isNew) {
            entityManager.persist(invocation);
        } else {
            testcaseInvocationRepository.save(invocation);
        }

        UUID keptId = invocation.getId();
        for (TestcaseInvocation extra : existing) {
            if (!extra.getId().equals(keptId)) {
                testcaseInvocationRepository.delete(extra);
            }
        }
        entityManager.flush();
    }

    private TestcaseInvocation resolveInvocation(List<TestcaseInvocation> existing, UUID dtoId) {
        if (dtoId != null) {
            return existing.stream()
                    .filter(inv -> dtoId.equals(inv.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return existing.size() == 1 ? existing.get(0) : null;
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
                                ChallengeMemberIds memberIds) {
        List<TestcaseAssertion> existing = testcaseAssertionRepository
                .findByTestcase_IdInOrderByOrderIndexAsc(List.of(testcase.getId()));
        Map<UUID, TestcaseAssertion> existingById = existing.stream()
                .collect(Collectors.toMap(TestcaseAssertion::getId, row -> row, (left, right) -> left));

        if (dtos == null || dtos.isEmpty()) {
            testcaseAssertionRepository.deleteAll(existing);
            return;
        }

        TestcaseInvocation invocation = testcaseInvocationRepository.findByTestcase_IdIn(List.of(testcase.getId()))
                .stream().findFirst().orElse(null);
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
            if (dto.invocationId() != null && invocation != null
                    && dto.invocationId().equals(invocation.getId())) {
                assertion.setInvocation(invocation);
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
        Set<UUID> constructorIds = new HashSet<>();
        Set<UUID> methodIds = new HashSet<>();
        Set<UUID> fieldIds = new HashSet<>();
        if (!classes.isEmpty()) {
            for (Constructor ctor : constructorRepository.findByClassEntityInWithDeclaration(classes)) {
                constructorIds.add(ctor.getId());
            }
            for (Method method : methodRepository.findByClassEntityInWithDeclaration(classes)) {
                methodIds.add(method.getId());
            }
            for (Field field : fieldRepository.findByClassEntityInWithDeclaration(classes)) {
                fieldIds.add(field.getId());
            }
        }
        return new ChallengeMemberIds(constructorIds, methodIds, fieldIds);
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

    private record ChallengeMemberIds(Set<UUID> constructorIds, Set<UUID> methodIds, Set<UUID> fieldIds) {}
}
