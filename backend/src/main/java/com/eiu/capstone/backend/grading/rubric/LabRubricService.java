package com.eiu.capstone.backend.grading.rubric;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ClassRelation;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.ConstructorDeclaration;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.FieldDeclaration;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInstance;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInstanceRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;

@Service
public class LabRubricService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final ClassRelationRepository classRelationRepository;
    private final TestcaseRepository testcaseRepository;
    private final TestcaseInvocationRepository testcaseInvocationRepository;
    private final TestcaseInstanceRepository testcaseInstanceRepository;
    private final TestcaseAssertionRepository testcaseAssertionRepository;

    public LabRubricService(ChallengeRepository challengeRepository,
                            ClassEntityRepository classEntityRepository,
                            FieldRepository fieldRepository,
                            MethodRepository methodRepository,
                            ConstructorRepository constructorRepository,
                            ParameterRepository parameterRepository,
                            ClassRelationRepository classRelationRepository,
                            TestcaseRepository testcaseRepository,
                            TestcaseInvocationRepository testcaseInvocationRepository,
                            TestcaseInstanceRepository testcaseInstanceRepository,
                            TestcaseAssertionRepository testcaseAssertionRepository) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.classRelationRepository = classRelationRepository;
        this.testcaseRepository = testcaseRepository;
        this.testcaseInvocationRepository = testcaseInvocationRepository;
        this.testcaseInstanceRepository = testcaseInstanceRepository;
        this.testcaseAssertionRepository = testcaseAssertionRepository;
    }

    public LabRubricSnapshot loadForLab(Lab lab) {
        List<Challenge> challenges = challengeRepository.findByLabOrderByChallengeNumberAsc(lab);
        if (challenges.isEmpty()) {
            return new LabRubricSnapshot(lab.getId(), Map.of());
        }

        List<ClassEntity> allClasses = classEntityRepository.findByChallengeInWithAttributes(challenges);
        List<Field> allFields = allClasses.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(allClasses);
        List<Method> allMethods = allClasses.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(allClasses);
        List<Constructor> allConstructors = allClasses.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(allClasses);

        List<Parameter> methodParams = allMethods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(allMethods);
        List<Parameter> constructorParams = allConstructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);
        List<ClassRelation> allRelations = allClasses.isEmpty() ? List.of()
                : classRelationRepository.findByClassEntityInWithEndpoints(allClasses);

        List<UUID> challengeIds = challenges.stream().map(Challenge::getId).toList();
        List<Testcase> allTestcases = challengeIds.isEmpty() ? List.of()
                : testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(challengeIds);
        List<UUID> testcaseIds = allTestcases.stream().map(Testcase::getId).toList();
        List<TestcaseInvocation> allInvocations = testcaseIds.isEmpty() ? List.of()
                : testcaseInvocationRepository.findByTestcase_IdIn(testcaseIds);
        List<TestcaseInstance> allInstances = testcaseIds.isEmpty() ? List.of()
                : testcaseInstanceRepository.findByTestcase_IdIn(testcaseIds);
        List<TestcaseAssertion> allAssertions = testcaseIds.isEmpty() ? List.of()
                : testcaseAssertionRepository.findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds);
        Map<UUID, TestcaseInvocation> invocationByTestcaseId = allInvocations.stream()
                .collect(Collectors.toMap(inv -> inv.getTestcase().getId(), inv -> inv, (a, b) -> a));
        Map<UUID, List<TestcaseInstance>> instancesByTestcaseId = allInstances.stream()
                .collect(Collectors.groupingBy(inst -> inst.getTestcase().getId()));
        Map<UUID, List<TestcaseAssertion>> assertionsByTestcaseId = allAssertions.stream()
                .collect(Collectors.groupingBy(a -> a.getTestcase().getId()));
        Map<UUID, String> classNameByClassId = allClasses.stream()
                .collect(Collectors.toMap(ClassEntity::getId, ClassEntity::getName));
        Map<UUID, String> classNameByConstructorId = new HashMap<>();
        for (Constructor constructor : allConstructors) {
            classNameByConstructorId.put(
                    constructor.getId(),
                    classNameByClassId.get(constructor.getClassEntity().getId()));
        }
        Map<UUID, String> classNameByMethodId = new HashMap<>();
        Map<UUID, Method> methodById = new HashMap<>();
        for (Method method : allMethods) {
            methodById.put(method.getId(), method);
            classNameByMethodId.put(
                    method.getId(),
                    classNameByClassId.get(method.getClassEntity().getId()));
        }
        Map<UUID, Field> fieldById = allFields.stream()
                .collect(Collectors.toMap(Field::getId, field -> field));
        Map<UUID, List<String>> paramTypesByMethod = RubricParameterMaps.byMethod(methodParams);
        Map<UUID, List<String>> paramTypesByConstructorId = RubricParameterMaps.byConstructor(constructorParams);
        TestcaseRubricContext testcaseContext = new TestcaseRubricContext(
                classNameByConstructorId,
                paramTypesByConstructorId,
                paramTypesByMethod,
                classNameByMethodId,
                methodById,
                fieldById);
        Map<UUID, List<Testcase>> testcasesByChallenge = allTestcases.stream()
                .collect(Collectors.groupingBy(t -> t.getChallenge().getId()));

        Map<UUID, List<Field>> fieldsByClass = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));

        Map<UUID, List<ClassEntity>> classesByChallenge = allClasses.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        Map<UUID, List<ClassRelation>> relationsByChallenge = allRelations.stream()
                .collect(Collectors.groupingBy(r -> r.getClassEntity().getChallenge().getId()));

        Map<Integer, ChallengeRubric> byNumber = new HashMap<>();
        for (Challenge challenge : challenges) {
            List<ClassEntity> challengeClasses = classesByChallenge.getOrDefault(challenge.getId(), List.of());
            List<ClassRubric> classRubrics = new ArrayList<>();
            for (ClassEntity classEntity : challengeClasses) {
                classRubrics.add(toClassRubric(classEntity,
                        fieldsByClass.getOrDefault(classEntity.getId(), List.of()),
                        methodsByClass.getOrDefault(classEntity.getId(), List.of()),
                        constructorsByClass.getOrDefault(classEntity.getId(), List.of()),
                        paramTypesByMethod,
                        paramTypesByConstructorId));
            }
            List<RelationRubric> relationRubrics = relationsByChallenge.getOrDefault(challenge.getId(), List.of())
                    .stream()
                    .map(this::toRelationRubric)
                    .toList();
            List<TestcaseRubric> testcaseRubrics = testcasesByChallenge.getOrDefault(challenge.getId(), List.of())
                    .stream()
                    .map(testcase -> toTestcaseRubric(
                            testcase,
                            invocationByTestcaseId.get(testcase.getId()),
                            instancesByTestcaseId.getOrDefault(testcase.getId(), List.of()),
                            assertionsByTestcaseId.getOrDefault(testcase.getId(), List.of()),
                            testcaseContext))
                    .toList();
            byNumber.put(challenge.getChallengeNumber(),
                    new ChallengeRubric(challenge.getId(), challenge.getChallengeNumber(), challenge.getName(),
                            classRubrics, relationRubrics, testcaseRubrics, challenge.isHasMmd(),
                            Math.max(1, challenge.getWeight()),
                            Math.max(1, challenge.getClassWeight()),
                            Math.max(1, challenge.getMmdWeight())));
        }

        return new LabRubricSnapshot(lab.getId(), Map.copyOf(byNumber));
    }

    private ClassRubric toClassRubric(ClassEntity classEntity,
                                      List<Field> fields,
                                      List<Method> methods,
                                      List<Constructor> constructors,
                                      Map<UUID, List<String>> paramTypesByMethod,
                                      Map<UUID, List<String>> paramTypesByConstructor) {
        List<FieldRubric> fieldRubrics = fields.stream()
                .map(f -> {
                    FieldDeclaration fd = f.getFieldDeclaration();
                    return new FieldRubric(f.getId(), f.getName(), fd.getScope().getName(), fd.getDataType());
                })
                .toList();

        List<MethodRubric> methodRubrics = methods.stream()
                .map(m -> {
                    MethodDeclaration md = m.getMethodDeclaration();
                    return new MethodRubric(
                            m.getId(),
                            m.getName(),
                            md.getScope().getName(),
                            md.getReturnType(),
                            md.isStatic(),
                            md.isAbstract(),
                            md.isFinal(),
                            paramTypesByMethod.getOrDefault(m.getId(), List.of()));
                })
                .toList();

        List<ConstructorRubric> constructorRubrics = constructors.stream()
                .map(c -> {
                    ConstructorDeclaration cd = c.getConstructorDeclaration();
                    return new ConstructorRubric(
                            c.getId(),
                            cd.getScope().getName(),
                            cd.isDefault(),
                            paramTypesByConstructor.getOrDefault(c.getId(), List.of()));
                })
                .toList();

        return new ClassRubric(
                classEntity.getId(),
                classEntity.getName(),
                classEntity.getScope().getName(),
                classEntity.getDeclaringType().getName(),
                classEntity.isAbstract(),
                fieldRubrics,
                methodRubrics,
                constructorRubrics,
                Math.max(1, classEntity.getWeight()));
    }

    private RelationRubric toRelationRubric(ClassRelation relation) {
        return new RelationRubric(
                relation.getId(),
                relation.getClassEntity().getId(),
                relation.getClassEntity().getName(),
                relation.getTargetClassEntity().getId(),
                relation.getTargetClassEntity().getName(),
                relation.getRelationType().getName());
    }

    private TestcaseRubric toTestcaseRubric(Testcase testcase,
                                            TestcaseInvocation invocation,
                                            List<TestcaseInstance> instances,
                                            List<TestcaseAssertion> assertions,
                                            TestcaseRubricContext context) {
        InvocationRubric invocationRubric = null;
        if (invocation != null) {
            if (invocation.getInvocationKind() == InvocationKind.CONSTRUCTOR) {
                UUID constructorId = invocation.getConstructor().getId();
                invocationRubric = new InvocationRubric(
                        invocation.getId(),
                        invocation.getInvocationKind(),
                        constructorId,
                        null,
                        context.classNameByConstructorId().get(constructorId),
                        null,
                        context.paramTypesByConstructorId().getOrDefault(constructorId, List.of()),
                        invocation.getParams(),
                        null,
                        null,
                        List.of(),
                        null);
            } else {
                invocationRubric = methodInvocationRubric(invocation, context);
            }
        }

        List<InstanceRubric> instanceRubrics = instances.stream()
                .sorted(Comparator.comparing(TestcaseInstance::getLabel))
                .map(inst -> {
                    UUID constructorId = inst.getConstructor().getId();
                    return new InstanceRubric(
                            inst.getId(),
                            inst.getLabel(),
                            constructorId,
                            context.classNameByConstructorId().get(constructorId),
                            context.paramTypesByConstructorId().getOrDefault(constructorId, List.of()),
                            inst.getParams());
                })
                .toList();

        List<AssertionRubric> assertionRubrics = assertions.stream()
                .map(assertion -> {
                    Field field = assertion.getField() != null
                            ? context.fieldById().get(assertion.getField().getId())
                            : null;
                    return new AssertionRubric(
                            assertion.getId(),
                            assertion.getAssertionKind(),
                            assertion.getInvocation() != null ? assertion.getInvocation().getId() : null,
                            field != null ? field.getId() : null,
                            field != null ? field.getName() : null,
                            field != null ? field.getFieldDeclaration().getDataType() : null,
                            assertion.getExpectedValue(),
                            assertion.getComparisonMode(),
                            assertion.getOrderIndex());
                })
                .toList();

        return new TestcaseRubric(
                testcase.getId(),
                testcase.getName(),
                testcase.getTestcaseType(),
                testcase.getComparisonMethod(),
                testcase.getWeight(),
                testcase.getOrderIndex(),
                testcase.isHidden(),
                invocationRubric,
                instanceRubrics,
                assertionRubrics);
    }

    private InvocationRubric methodInvocationRubric(TestcaseInvocation invocation, TestcaseRubricContext context) {
        UUID methodId = invocation.getMethod().getId();
        Method method = context.methodById().get(methodId);
        UUID receiverConstructorId = invocation.getReceiverConstructor() != null
                ? invocation.getReceiverConstructor().getId()
                : null;
        String receiverClassName = receiverConstructorId != null
                ? context.classNameByConstructorId().get(receiverConstructorId)
                : null;
        List<String> receiverParameterTypes = receiverConstructorId != null
                ? context.paramTypesByConstructorId().getOrDefault(receiverConstructorId, List.of())
                : List.of();
        return new InvocationRubric(
                invocation.getId(),
                invocation.getInvocationKind(),
                null,
                methodId,
                context.classNameByMethodId().get(methodId),
                method != null ? method.getName() : null,
                context.paramTypesByMethodId().getOrDefault(methodId, List.of()),
                invocation.getParams(),
                receiverConstructorId,
                receiverClassName,
                receiverParameterTypes,
                invocation.getReceiverParams());
    }

    private record TestcaseRubricContext(
            Map<UUID, String> classNameByConstructorId,
            Map<UUID, List<String>> paramTypesByConstructorId,
            Map<UUID, List<String>> paramTypesByMethodId,
            Map<UUID, String> classNameByMethodId,
            Map<UUID, Method> methodById,
            Map<UUID, Field> fieldById) {}
}
