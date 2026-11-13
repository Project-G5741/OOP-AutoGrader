package com.eiu.capstone.backend.grading.rubric;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@Component
public class TestcaseRubricAssembler {

    private final TestcaseRubricService testcaseRubricService;
    private final LabRubricCache labRubricCache;

    public TestcaseRubricAssembler(TestcaseRubricService testcaseRubricService) {
        this(testcaseRubricService, null);
    }

    @Autowired
    public TestcaseRubricAssembler(
            TestcaseRubricService testcaseRubricService,
            LabRubricCache labRubricCache) {
        this.testcaseRubricService = testcaseRubricService;
        this.labRubricCache = labRubricCache;
    }

    public TestcaseRubric assemble(UUID challengeId, TestcaseStructureDTO dto) {
        return assemble(null, challengeId, dto);
    }

    /**
     * Dry-run assemble. When {@code labId} is set, lab ownership is checked only on a cold
     * catalog load; warm cache hits are in-memory (no Neon).
     */
    public TestcaseRubric assemble(UUID labId, UUID challengeId, TestcaseStructureDTO dto) {
        testcaseRubricService.validatePayload(labId, challengeId, dto);
        RubricMemberMaps maps = testcaseRubricService.dryRunMemberMaps(labId, challengeId);
        ChallengeRubric aliasSource = labId != null && labRubricCache != null
                ? labRubricCache.get(labId).challengeById(challengeId).orElse(null)
                : null;
        UUID testcaseId = dto.id() != null ? dto.id() : UUID.randomUUID();

        List<InvocationStructureDTO> steps = TestcaseRubricService.resolvedInvocations(dto);
        List<InvocationRubric> invocationRubrics = steps.stream()
                .map(step -> toInvocationRubric(step, maps, aliasSource))
                .toList();
        InvocationRubric singular = invocationRubrics.isEmpty() ? null : invocationRubrics.get(0);
        Map<UUID, InvocationRubric> invocationById = invocationRubrics.stream()
                .collect(Collectors.toMap(InvocationRubric::id, row -> row, (left, right) -> left));

        List<InstanceRubric> instances = dto.instances() == null ? List.of() : dto.instances().stream()
                .sorted(Comparator.comparing(InstanceStructureDTO::label))
                .map(inst -> {
                    UUID constructorId = inst.constructorId();
                    return new InstanceRubric(
                            inst.id() != null ? inst.id() : UUID.randomUUID(),
                            inst.label(),
                            constructorId,
                            maps.classNameByConstructorId().get(constructorId),
                            maps.paramTypesByConstructorId().getOrDefault(constructorId, List.of()),
                            inst.params() != null ? inst.params() : "[]");
                })
                .toList();

        List<AssertionRubric> assertions = dto.assertions() == null ? List.of() : dto.assertions().stream()
                .map(a -> toAssertionRubric(a, maps, aliasSource, singular, invocationById))
                .toList();

        OopPrincipleTag tag = dto.oopPrincipleTag();
        return new TestcaseRubric(
                testcaseId,
                dto.name(),
                dto.testcaseType(),
                dto.comparisonMethod(),
                dto.weight(),
                dto.orderIndex(),
                dto.hidden(),
                singular,
                instances,
                assertions,
                invocationRubrics,
                tag);
    }

    private AssertionRubric toAssertionRubric(AssertionStructureDTO dto,
                                              RubricMemberMaps maps,
                                              ChallengeRubric aliasSource,
                                              InvocationRubric singular,
                                              Map<UUID, InvocationRubric> invocationById) {
        Field field = dto.fieldId() != null ? maps.fieldById().get(dto.fieldId()) : null;
        FieldRubric fieldAliases = findField(aliasSource, dto.fieldId());
        UUID invocationId = dto.invocationId() != null && invocationById.containsKey(dto.invocationId())
                ? dto.invocationId()
                : (singular != null ? singular.id() : null);
        return new AssertionRubric(
                dto.id() != null ? dto.id() : UUID.randomUUID(),
                dto.assertionKind(),
                invocationId,
                field != null ? field.getId() : null,
                field != null ? field.getName() : null,
                field != null ? field.getFieldDeclaration().getDataType() : null,
                dto.expectedValue(),
                dto.comparisonMode(),
                dto.orderIndex(),
                fieldAliases != null ? fieldAliases.names() : null);
    }

    private InvocationRubric toInvocationRubric(
            InvocationStructureDTO dto,
            RubricMemberMaps maps,
            ChallengeRubric aliasSource) {
        UUID invocationId = dto.id() != null ? dto.id() : UUID.randomUUID();
        UUID dispatchClassId = dto.dispatchClassId();
        String dispatchClassName = dispatchClassId != null ? maps.classNameByClassId().get(dispatchClassId) : null;
        String instanceName = dto.instanceName() != null && !dto.instanceName().isBlank()
                ? dto.instanceName().trim() : null;
        if (dto.invocationKind() == InvocationKind.CONSTRUCTOR) {
            UUID constructorId = dto.constructorId();
            boolean implicitDefault = constructorId == null;
            String className = implicitDefault
                    ? maps.classNameByClassId().get(dispatchClassId)
                    : maps.classNameByConstructorId().get(constructorId);
            ConstructorRubric constructorAliases = findConstructor(aliasSource, constructorId);
            int repeat = TestcaseRubricService.normalizedRepeatCount(dto.repeatCount());
            return new InvocationRubric(
                    invocationId,
                    dto.invocationKind(),
                    constructorId,
                    null,
                    className,
                    null,
                    implicitDefault
                            ? List.of()
                            : maps.paramTypesByConstructorId().getOrDefault(constructorId, List.of()),
                    implicitDefault ? "[]" : (dto.params() != null ? dto.params() : "[]"),
                    null,
                    null,
                    List.of(),
                    null,
                    instanceName,
                    dispatchClassId,
                    dispatchClassName,
                    InvocationRubric.resultTypeNameForConstructor(className),
                    1,
                    null,
                    constructorAliases != null
                            ? constructorAliases.parameters().stream().map(ParameterRubric::dataTypes).toList()
                            : null,
                    null);
        }
        UUID methodId = dto.methodId();
        Method method = maps.methodById().get(methodId);
        MethodRubric methodAliases = findMethod(aliasSource, methodId);
        UUID receiverConstructorId = dto.receiverConstructorId();
        ConstructorRubric receiverAliases = findConstructor(aliasSource, receiverConstructorId);
        int repeat = TestcaseRubricService.normalizedRepeatCount(dto.repeatCount());
        return new InvocationRubric(
                invocationId,
                dto.invocationKind(),
                null,
                methodId,
                maps.classNameByMethodId().get(methodId),
                method != null ? method.getName() : null,
                maps.paramTypesByMethodId().getOrDefault(methodId, List.of()),
                dto.params() != null ? dto.params() : "[]",
                receiverConstructorId,
                receiverConstructorId != null
                        ? maps.classNameByConstructorId().get(receiverConstructorId) : null,
                receiverConstructorId != null
                        ? maps.paramTypesByConstructorId().getOrDefault(receiverConstructorId, List.of())
                        : List.of(),
                dto.receiverParams() != null ? dto.receiverParams() : "[]",
                instanceName,
                dispatchClassId,
                dispatchClassName,
                InvocationRubric.resultTypeNameForMethod(method),
                repeat,
                methodAliases != null ? methodAliases.names() : null,
                methodAliases != null
                        ? methodAliases.parameters().stream().map(ParameterRubric::dataTypes).toList()
                        : null,
                receiverAliases != null
                        ? receiverAliases.parameters().stream().map(ParameterRubric::dataTypes).toList()
                        : null);
    }

    private static FieldRubric findField(ChallengeRubric challenge, UUID fieldId) {
        if (challenge == null || fieldId == null) {
            return null;
        }
        return challenge.classes().stream()
                .flatMap(cls -> cls.fields().stream())
                .filter(field -> fieldId.equals(field.id()))
                .findFirst()
                .orElse(null);
    }

    private static MethodRubric findMethod(ChallengeRubric challenge, UUID methodId) {
        if (challenge == null || methodId == null) {
            return null;
        }
        return challenge.classes().stream()
                .flatMap(cls -> cls.methods().stream())
                .filter(method -> methodId.equals(method.id()))
                .findFirst()
                .orElse(null);
    }

    private static ConstructorRubric findConstructor(ChallengeRubric challenge, UUID constructorId) {
        if (challenge == null || constructorId == null) {
            return null;
        }
        return challenge.classes().stream()
                .flatMap(cls -> cls.constructors().stream())
                .filter(constructor -> constructorId.equals(constructor.id()))
                .findFirst()
                .orElse(null);
    }
}
