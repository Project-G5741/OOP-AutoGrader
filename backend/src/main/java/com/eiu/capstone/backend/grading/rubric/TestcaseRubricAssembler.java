package com.eiu.capstone.backend.grading.rubric;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@Component
public class TestcaseRubricAssembler {

    private final ClassEntityRepository classEntityRepository;
    private final ConstructorRepository constructorRepository;
    private final MethodRepository methodRepository;
    private final FieldRepository fieldRepository;
    private final ParameterRepository parameterRepository;
    private final TestcaseRubricService testcaseRubricService;

    public TestcaseRubricAssembler(ClassEntityRepository classEntityRepository,
                                   ConstructorRepository constructorRepository,
                                   MethodRepository methodRepository,
                                   FieldRepository fieldRepository,
                                   ParameterRepository parameterRepository,
                                   TestcaseRubricService testcaseRubricService) {
        this.classEntityRepository = classEntityRepository;
        this.constructorRepository = constructorRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
        this.parameterRepository = parameterRepository;
        this.testcaseRubricService = testcaseRubricService;
    }

    public TestcaseRubric assemble(UUID challengeId, TestcaseStructureDTO dto) {
        testcaseRubricService.validatePayload(challengeId, dto);
        MemberMaps maps = loadMemberMaps(challengeId);
        UUID testcaseId = dto.id() != null ? dto.id() : UUID.randomUUID();

        final InvocationRubric invocationRubric = dto.invocation() != null
                ? toInvocationRubric(dto.invocation(), maps)
                : null;

        List<InstanceRubric> instances = dto.instances() == null ? List.of() : dto.instances().stream()
                .sorted(Comparator.comparing(InstanceStructureDTO::label))
                .map(inst -> {
                    UUID constructorId = inst.constructorId();
                    return new InstanceRubric(
                            inst.id() != null ? inst.id() : UUID.randomUUID(),
                            inst.label(),
                            constructorId,
                            maps.classNameByConstructorId.get(constructorId),
                            maps.paramTypesByConstructorId.getOrDefault(constructorId, List.of()),
                            inst.params() != null ? inst.params() : "[]");
                })
                .toList();

        List<AssertionRubric> assertions = dto.assertions() == null ? List.of() : dto.assertions().stream()
                .map(a -> toAssertionRubric(a, maps, invocationRubric))
                .toList();

        return new TestcaseRubric(
                testcaseId,
                dto.name(),
                dto.testcaseType(),
                dto.comparisonMethod(),
                dto.weight(),
                dto.orderIndex(),
                dto.hidden(),
                invocationRubric,
                instances,
                assertions);
    }

    private AssertionRubric toAssertionRubric(AssertionStructureDTO dto,
                                              MemberMaps maps,
                                              InvocationRubric invocationRubric) {
        Field field = dto.fieldId() != null ? maps.fieldById.get(dto.fieldId()) : null;
        return new AssertionRubric(
                dto.id() != null ? dto.id() : UUID.randomUUID(),
                dto.assertionKind(),
                invocationRubric != null ? invocationRubric.id() : null,
                field != null ? field.getId() : null,
                field != null ? field.getName() : null,
                field != null ? field.getFieldDeclaration().getDataType() : null,
                dto.expectedValue(),
                dto.comparisonMode(),
                dto.orderIndex());
    }

    private InvocationRubric toInvocationRubric(InvocationStructureDTO dto, MemberMaps maps) {
        UUID invocationId = dto.id() != null ? dto.id() : UUID.randomUUID();
        if (dto.invocationKind() == InvocationKind.CONSTRUCTOR) {
            UUID constructorId = dto.constructorId();
            return new InvocationRubric(
                    invocationId,
                    dto.invocationKind(),
                    constructorId,
                    null,
                    maps.classNameByConstructorId.get(constructorId),
                    null,
                    maps.paramTypesByConstructorId.getOrDefault(constructorId, List.of()),
                    dto.params() != null ? dto.params() : "[]",
                    null,
                    null,
                    List.of(),
                    null);
        }
        UUID methodId = dto.methodId();
        Method method = maps.methodById.get(methodId);
        UUID receiverConstructorId = dto.receiverConstructorId();
        return new InvocationRubric(
                invocationId,
                dto.invocationKind(),
                null,
                methodId,
                maps.classNameByMethodId.get(methodId),
                method != null ? method.getName() : null,
                maps.paramTypesByMethodId.getOrDefault(methodId, List.of()),
                dto.params() != null ? dto.params() : "[]",
                receiverConstructorId,
                receiverConstructorId != null
                        ? maps.classNameByConstructorId.get(receiverConstructorId) : null,
                receiverConstructorId != null
                        ? maps.paramTypesByConstructorId.getOrDefault(receiverConstructorId, List.of())
                        : List.of(),
                dto.receiverParams() != null ? dto.receiverParams() : "[]");
    }

    private MemberMaps loadMemberMaps(UUID challengeId) {
        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challengeId);
        List<Constructor> constructors = classes.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> methods = classes.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Field> fields = classes.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Parameter> methodParams = methods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(methods);
        List<Parameter> constructorParams = constructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(constructors);

        Map<UUID, String> classNameByClassId = new HashMap<>();
        for (ClassEntity cls : classes) {
            classNameByClassId.put(cls.getId(), cls.getName());
        }
        Map<UUID, String> classNameByConstructorId = new HashMap<>();
        for (Constructor constructor : constructors) {
            classNameByConstructorId.put(
                    constructor.getId(),
                    classNameByClassId.get(constructor.getClassEntity().getId()));
        }
        Map<UUID, String> classNameByMethodId = new HashMap<>();
        Map<UUID, Method> methodById = new HashMap<>();
        for (Method method : methods) {
            methodById.put(method.getId(), method);
            classNameByMethodId.put(method.getId(), classNameByClassId.get(method.getClassEntity().getId()));
        }
        Map<UUID, Field> fieldById = new HashMap<>();
        for (Field field : fields) {
            fieldById.put(field.getId(), field);
        }

        return new MemberMaps(
                classNameByConstructorId,
                RubricParameterMaps.byConstructor(constructorParams),
                RubricParameterMaps.byMethod(methodParams),
                classNameByMethodId,
                methodById,
                fieldById);
    }

    private record MemberMaps(
            Map<UUID, String> classNameByConstructorId,
            Map<UUID, List<String>> paramTypesByConstructorId,
            Map<UUID, List<String>> paramTypesByMethodId,
            Map<UUID, String> classNameByMethodId,
            Map<UUID, Method> methodById,
            Map<UUID, Field> fieldById) {}
}
