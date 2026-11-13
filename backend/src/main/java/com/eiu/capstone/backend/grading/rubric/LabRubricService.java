package com.eiu.capstone.backend.grading.rubric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.eiu.capstone.backend.model.FieldDeclarationNameAlias;
import com.eiu.capstone.backend.model.FieldDeclarationTypeAlias;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.MethodDeclarationNameAlias;
import com.eiu.capstone.backend.model.MethodDeclarationReturnTypeAlias;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.ParameterNameAlias;
import com.eiu.capstone.backend.model.ParameterTypeAlias;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInstance;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationNameAliasRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationTypeAliasRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationNameAliasRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationReturnTypeAliasRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.ParameterNameAliasRepository;
import com.eiu.capstone.backend.repository.ParameterTypeAliasRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
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
    private final TestcaseAssertionRepository testcaseAssertionRepository;
    private final FieldDeclarationNameAliasRepository fieldNameAliasRepository;
    private final FieldDeclarationTypeAliasRepository fieldTypeAliasRepository;
    private final MethodDeclarationNameAliasRepository methodNameAliasRepository;
    private final MethodDeclarationReturnTypeAliasRepository methodReturnTypeAliasRepository;
    private final ParameterNameAliasRepository parameterNameAliasRepository;
    private final ParameterTypeAliasRepository parameterTypeAliasRepository;

    public LabRubricService(ChallengeRepository challengeRepository,
                            ClassEntityRepository classEntityRepository,
                            FieldRepository fieldRepository,
                            MethodRepository methodRepository,
                            ConstructorRepository constructorRepository,
                            ParameterRepository parameterRepository,
                            ClassRelationRepository classRelationRepository,
                            TestcaseRepository testcaseRepository,
                            TestcaseInvocationRepository testcaseInvocationRepository,
                            TestcaseAssertionRepository testcaseAssertionRepository,
                            FieldDeclarationNameAliasRepository fieldNameAliasRepository,
                            FieldDeclarationTypeAliasRepository fieldTypeAliasRepository,
                            MethodDeclarationNameAliasRepository methodNameAliasRepository,
                            MethodDeclarationReturnTypeAliasRepository methodReturnTypeAliasRepository,
                            ParameterNameAliasRepository parameterNameAliasRepository,
                            ParameterTypeAliasRepository parameterTypeAliasRepository) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.classRelationRepository = classRelationRepository;
        this.testcaseRepository = testcaseRepository;
        this.testcaseInvocationRepository = testcaseInvocationRepository;
        this.testcaseAssertionRepository = testcaseAssertionRepository;
        this.fieldNameAliasRepository = fieldNameAliasRepository;
        this.fieldTypeAliasRepository = fieldTypeAliasRepository;
        this.methodNameAliasRepository = methodNameAliasRepository;
        this.methodReturnTypeAliasRepository = methodReturnTypeAliasRepository;
        this.parameterNameAliasRepository = parameterNameAliasRepository;
        this.parameterTypeAliasRepository = parameterTypeAliasRepository;
    }

    public LabRubricSnapshot loadForLab(Lab lab) {
        return loadForLabs(List.of(lab)).get(lab.getId());
    }

    /**
     * Load rubrics for many labs with one batched query set (same round-trips as a single
     * {@link #loadForLab}, not one set per lab). Every non-null lab id is present in the result;
     * labs with no challenges get an empty snapshot.
     */
    public Map<UUID, LabRubricSnapshot> loadForLabs(Collection<Lab> labs) {
        if (labs == null || labs.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<UUID, Lab> unique = new LinkedHashMap<>();
        for (Lab lab : labs) {
            if (lab != null && lab.getId() != null) {
                unique.putIfAbsent(lab.getId(), lab);
            }
        }
        if (unique.isEmpty()) {
            return Map.of();
        }

        List<UUID> labIds = List.copyOf(unique.keySet());
        List<Challenge> challenges = challengeRepository.findByLab_IdInOrderByChallengeNumberAsc(labIds);
        Map<UUID, LabRubricSnapshot> assembled = assembleSnapshots(challenges);

        Map<UUID, LabRubricSnapshot> result = new LinkedHashMap<>(unique.size());
        for (UUID labId : labIds) {
            result.put(labId, assembled.getOrDefault(labId, new LabRubricSnapshot(labId, Map.of())));
        }
        return Map.copyOf(result);
    }

    private Map<UUID, LabRubricSnapshot> assembleSnapshots(List<Challenge> challenges) {
        if (challenges.isEmpty()) {
            return Map.of();
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
        List<UUID> fieldDeclarationIds = allFields.stream()
                .map(Field::getFieldDeclaration)
                .map(FieldDeclaration::getId)
                .toList();
        List<UUID> methodDeclarationIds = allMethods.stream()
                .map(Method::getMethodDeclaration)
                .map(MethodDeclaration::getId)
                .toList();
        List<UUID> parameterIds = java.util.stream.Stream.concat(
                        methodParams.stream(), constructorParams.stream())
                .map(Parameter::getId)
                .toList();
        Map<UUID, List<String>> fieldNamesByDeclaration = fieldDeclarationIds.isEmpty()
                ? Map.of()
                : fieldNameAliasRepository
                        .findByFieldDeclaration_IdInOrderByOrderIndexAsc(fieldDeclarationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getFieldDeclaration().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(FieldDeclarationNameAlias::getValue, Collectors.toList())));
        Map<UUID, List<String>> fieldTypesByDeclaration = fieldDeclarationIds.isEmpty()
                ? Map.of()
                : fieldTypeAliasRepository
                        .findByFieldDeclaration_IdInOrderByOrderIndexAsc(fieldDeclarationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getFieldDeclaration().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(FieldDeclarationTypeAlias::getValue, Collectors.toList())));
        Map<UUID, List<String>> methodNamesByDeclaration = methodDeclarationIds.isEmpty()
                ? Map.of()
                : methodNameAliasRepository
                        .findByMethodDeclaration_IdInOrderByOrderIndexAsc(methodDeclarationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getMethodDeclaration().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(MethodDeclarationNameAlias::getValue, Collectors.toList())));
        Map<UUID, List<String>> methodReturnsByDeclaration = methodDeclarationIds.isEmpty()
                ? Map.of()
                : methodReturnTypeAliasRepository
                        .findByMethodDeclaration_IdInOrderByOrderIndexAsc(methodDeclarationIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getMethodDeclaration().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        MethodDeclarationReturnTypeAlias::getValue, Collectors.toList())));
        Map<UUID, List<String>> parameterNamesById = parameterIds.isEmpty()
                ? Map.of()
                : parameterNameAliasRepository.findByParameter_IdInOrderByOrderIndexAsc(parameterIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getParameter().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(ParameterNameAlias::getValue, Collectors.toList())));
        Map<UUID, List<String>> parameterTypesById = parameterIds.isEmpty()
                ? Map.of()
                : parameterTypeAliasRepository.findByParameter_IdInOrderByOrderIndexAsc(parameterIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                alias -> alias.getParameter().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(ParameterTypeAlias::getValue, Collectors.toList())));
        List<ClassRelation> allRelations = allClasses.isEmpty() ? List.of()
                : classRelationRepository.findByClassEntityInWithEndpoints(allClasses);

        List<UUID> challengeIds = challenges.stream().map(Challenge::getId).toList();
        List<Testcase> allTestcases = challengeIds.isEmpty() ? List.of()
                : testcaseRepository.findByChallenge_IdInOrderByOrderIndexAsc(challengeIds);
        List<UUID> testcaseIds = allTestcases.stream().map(Testcase::getId).toList();
        List<TestcaseInvocation> allInvocations = testcaseIds.isEmpty() ? List.of()
                : testcaseInvocationRepository.findByTestcase_IdIn(testcaseIds);
        List<TestcaseInstance> allInstances = List.of();
        List<TestcaseAssertion> allAssertions = testcaseIds.isEmpty() ? List.of()
                : testcaseAssertionRepository.findByTestcase_IdInOrderByOrderIndexAsc(testcaseIds);
        Map<UUID, List<TestcaseInvocation>> invocationsByTestcaseId = allInvocations.stream()
                .collect(Collectors.groupingBy(inv -> inv.getTestcase().getId()));
        Map<UUID, List<TestcaseInstance>> instancesByTestcaseId = allInstances.stream()
                .collect(Collectors.groupingBy(inst -> inst.getTestcase().getId()));
        Map<UUID, List<TestcaseAssertion>> assertionsByTestcaseId = allAssertions.stream()
                .collect(Collectors.groupingBy(a -> a.getTestcase().getId()));
        Map<UUID, String> classNameByClassId = allClasses.stream()
                .collect(Collectors.toMap(ClassEntity::getId, RubricMemberMaps::invokeBinaryName));
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
        Map<UUID, List<ParameterRubric>> paramAliasesByMethod = RubricParameterMaps.aliasesByMethod(
                methodParams, parameterNamesById, parameterTypesById);
        Map<UUID, List<ParameterRubric>> paramAliasesByConstructor = RubricParameterMaps.aliasesByConstructor(
                constructorParams, parameterNamesById, parameterTypesById);
        Map<UUID, List<String>> fieldNameAliasesById = allFields.stream()
                .collect(Collectors.toMap(
                        Field::getId,
                        field -> fieldNamesByDeclaration.getOrDefault(
                                field.getFieldDeclaration().getId(), List.of(field.getName()))));
        Map<UUID, List<String>> methodNameAliasesById = allMethods.stream()
                .collect(Collectors.toMap(
                        Method::getId,
                        method -> methodNamesByDeclaration.getOrDefault(
                                method.getMethodDeclaration().getId(), List.of(method.getName()))));
        Map<UUID, List<List<String>>> paramTypeAliasesByMethod = paramAliasesByMethod.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(ParameterRubric::dataTypes).toList()));
        Map<UUID, List<List<String>>> paramTypeAliasesByConstructor = paramAliasesByConstructor.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(ParameterRubric::dataTypes).toList()));
        TestcaseRubricContext testcaseContext = new TestcaseRubricContext(
                classNameByClassId,
                classNameByConstructorId,
                paramTypesByConstructorId,
                paramTypesByMethod,
                classNameByMethodId,
                methodById,
                fieldById,
                fieldNameAliasesById,
                methodNameAliasesById,
                paramTypeAliasesByMethod,
                paramTypeAliasesByConstructor);
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

        Map<UUID, List<Challenge>> challengesByLab = challenges.stream()
                .collect(Collectors.groupingBy(c -> c.getLab().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<UUID, LabRubricSnapshot> byLab = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<Challenge>> labChallenges : challengesByLab.entrySet()) {
            Map<Integer, ChallengeRubric> byNumber = new HashMap<>();
            for (Challenge challenge : labChallenges.getValue()) {
                List<ClassEntity> challengeClasses = classesByChallenge.getOrDefault(challenge.getId(), List.of());
                List<ClassRubric> classRubrics = new ArrayList<>();
                for (ClassEntity classEntity : challengeClasses) {
                    classRubrics.add(toClassRubric(classEntity,
                            fieldsByClass.getOrDefault(classEntity.getId(), List.of()),
                            methodsByClass.getOrDefault(classEntity.getId(), List.of()),
                            constructorsByClass.getOrDefault(classEntity.getId(), List.of()),
                            paramTypesByMethod,
                            paramTypesByConstructorId,
                            paramAliasesByMethod,
                            paramAliasesByConstructor,
                            fieldNamesByDeclaration,
                            fieldTypesByDeclaration,
                            methodNamesByDeclaration,
                            methodReturnsByDeclaration));
                }
                List<RelationRubric> relationRubrics = relationsByChallenge.getOrDefault(challenge.getId(), List.of())
                        .stream()
                        .map(this::toRelationRubric)
                        .toList();
                List<TestcaseRubric> testcaseRubrics = testcasesByChallenge.getOrDefault(challenge.getId(), List.of())
                        .stream()
                        .map(testcase -> toTestcaseRubric(
                                testcase,
                                invocationsByTestcaseId.getOrDefault(testcase.getId(), List.of()),
                                instancesByTestcaseId.getOrDefault(testcase.getId(), List.of()),
                                assertionsByTestcaseId.getOrDefault(testcase.getId(), List.of()),
                                testcaseContext))
                        .toList();
                byNumber.put(challenge.getChallengeNumber(),
                        new ChallengeRubric(challenge.getId(), challenge.getChallengeNumber(), challenge.getName(),
                                classRubrics, relationRubrics, testcaseRubrics, challenge.isHasMmd(),
                                challenge.isHasClass(),
                                Math.max(1, challenge.getWeight()),
                                Math.max(1, challenge.getClassWeight()),
                                Math.max(1, challenge.getMmdWeight()),
                                Math.max(1, challenge.getTestcaseWeight())));
            }
            byLab.put(labChallenges.getKey(), new LabRubricSnapshot(labChallenges.getKey(), Map.copyOf(byNumber)));
        }
        return byLab;
    }

    private ClassRubric toClassRubric(ClassEntity classEntity,
                                      List<Field> fields,
                                      List<Method> methods,
                                      List<Constructor> constructors,
                                      Map<UUID, List<String>> paramTypesByMethod,
                                      Map<UUID, List<String>> paramTypesByConstructor,
                                      Map<UUID, List<ParameterRubric>> paramAliasesByMethod,
                                      Map<UUID, List<ParameterRubric>> paramAliasesByConstructor,
                                      Map<UUID, List<String>> fieldNamesByDeclaration,
                                      Map<UUID, List<String>> fieldTypesByDeclaration,
                                      Map<UUID, List<String>> methodNamesByDeclaration,
                                      Map<UUID, List<String>> methodReturnsByDeclaration) {
        List<FieldRubric> fieldRubrics = fields.stream()
                .map(f -> {
                    FieldDeclaration fd = f.getFieldDeclaration();
                    String scope = fd.getScope() == null ? null : fd.getScope().getName();
                    return new FieldRubric(
                            f.getId(),
                            fieldNamesByDeclaration.getOrDefault(fd.getId(), List.of(f.getName())),
                            scope,
                            fieldTypesByDeclaration.getOrDefault(
                                    fd.getId(), java.util.Collections.singletonList(fd.getDataType())),
                            fd.isStatic(),
                            fd.isFinal());
                })
                .toList();

        List<MethodRubric> methodRubrics = methods.stream()
                .map(m -> {
                    MethodDeclaration md = m.getMethodDeclaration();
                    return new MethodRubric(
                            m.getId(),
                            methodNamesByDeclaration.getOrDefault(md.getId(), List.of(m.getName())),
                            md.getScope().getName(),
                            methodReturnsByDeclaration.getOrDefault(md.getId(), List.of(md.getReturnType())),
                            md.isStatic(),
                            md.isAbstract(),
                            md.isFinal(),
                            paramAliasesByMethod.getOrDefault(m.getId(), List.of()));
                })
                .toList();

        List<ConstructorRubric> constructorRubrics = constructors.stream()
                .map(c -> {
                    ConstructorDeclaration cd = c.getConstructorDeclaration();
                    return new ConstructorRubric(
                            c.getId(),
                            cd.getScope().getName(),
                            cd.isDefault(),
                            paramTypesByConstructor.getOrDefault(c.getId(), List.of()),
                            paramAliasesByConstructor.getOrDefault(c.getId(), List.of()));
                })
                .toList();

        return new ClassRubric(
                classEntity.getId(),
                classEntity.getName(),
                classEntity.getOuterClass() != null ? classEntity.getOuterClass().getName() : null,
                classEntity.getScope().getName(),
                classEntity.getDeclaringType().getName(),
                classEntity.isAbstract(),
                classEntity.isStatic(),
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
                                            List<TestcaseInvocation> invocations,
                                            List<TestcaseInstance> instances,
                                            List<TestcaseAssertion> assertions,
                                            TestcaseRubricContext context) {
        List<InvocationRubric> invocationRubrics = invocations.stream()
                .sorted(Comparator.comparingInt(TestcaseInvocation::getOrderIndex))
                .map(invocation -> toInvocationRubric(invocation, context))
                .toList();
        InvocationRubric invocationRubric = invocationRubrics.isEmpty() ? null : invocationRubrics.get(0);

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
                            assertion.getOrderIndex(),
                            field != null
                                    ? context.fieldNameAliasesById().getOrDefault(
                                            field.getId(), List.of(field.getName()))
                                    : List.of());
                })
                .toList();

        OopPrincipleTag tag = OopPrincipleTag.Unit;
        return new TestcaseRubric(
                testcase.getId(),
                testcase.getName(),
                testcase.getTestcaseType(),
                null,
                1,
                testcase.getOrderIndex(),
                testcase.isHidden(),
                invocationRubric,
                instanceRubrics,
                assertionRubrics,
                invocationRubrics,
                tag);
    }

    private InvocationRubric toInvocationRubric(TestcaseInvocation invocation, TestcaseRubricContext context) {
        UUID dispatchClassId = invocation.getDispatchClass() != null
                ? invocation.getDispatchClass().getId() : null;
        String dispatchClassName = dispatchClassId != null
                ? context.classNameByClassId().get(dispatchClassId) : null;
        String instanceName = invocation.getInstanceName();
        if (invocation.getInvocationKind() == InvocationKind.CONSTRUCTOR) {
            boolean implicitDefault = invocation.getConstructor() == null;
            UUID constructorId = implicitDefault ? null : invocation.getConstructor().getId();
            String className = implicitDefault
                    ? dispatchClassName
                    : context.classNameByConstructorId().get(constructorId);
            return new InvocationRubric(
                    invocation.getId(),
                    invocation.getInvocationKind(),
                    constructorId,
                    null,
                    className,
                    null,
                    implicitDefault
                            ? List.of()
                            : context.paramTypesByConstructorId().getOrDefault(constructorId, List.of()),
                    implicitDefault ? "[]" : invocation.getParams(),
                    null,
                    null,
                    List.of(),
                    null,
                    instanceName,
                    dispatchClassId,
                    dispatchClassName,
                    InvocationRubric.resultTypeNameForConstructor(className),
                    invocation.getRepeatCount(),
                    null,
                    implicitDefault
                            ? List.of()
                            : context.paramTypeAliasesByConstructorId()
                                    .getOrDefault(constructorId, List.of()));
        }
        return methodInvocationRubric(invocation, context, instanceName, dispatchClassId, dispatchClassName);
    }

    private InvocationRubric methodInvocationRubric(TestcaseInvocation invocation,
                                                    TestcaseRubricContext context,
                                                    String instanceName,
                                                    UUID dispatchClassId,
                                                    String dispatchClassName) {
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
                invocation.getReceiverParams(),
                instanceName,
                dispatchClassId,
                dispatchClassName,
                InvocationRubric.resultTypeNameForMethod(method),
                invocation.getRepeatCount(),
                context.methodNameAliasesById().getOrDefault(
                        methodId, method != null ? List.of(method.getName()) : List.of()),
                context.paramTypeAliasesByMethodId().getOrDefault(methodId, List.of()));
    }

    private record TestcaseRubricContext(
            Map<UUID, String> classNameByClassId,
            Map<UUID, String> classNameByConstructorId,
            Map<UUID, List<String>> paramTypesByConstructorId,
            Map<UUID, List<String>> paramTypesByMethodId,
            Map<UUID, String> classNameByMethodId,
            Map<UUID, Method> methodById,
            Map<UUID, Field> fieldById,
            Map<UUID, List<String>> fieldNameAliasesById,
            Map<UUID, List<String>> methodNameAliasesById,
            Map<UUID, List<List<String>>> paramTypeAliasesByMethodId,
            Map<UUID, List<List<String>>> paramTypeAliasesByConstructorId) {}
}
