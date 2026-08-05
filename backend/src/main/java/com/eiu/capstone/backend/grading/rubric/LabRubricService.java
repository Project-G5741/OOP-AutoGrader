package com.eiu.capstone.backend.grading.rubric;

import java.util.ArrayList;
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
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;

@Service
public class LabRubricService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final ClassRelationRepository classRelationRepository;

    public LabRubricService(ChallengeRepository challengeRepository,
                            ClassEntityRepository classEntityRepository,
                            FieldRepository fieldRepository,
                            MethodRepository methodRepository,
                            ConstructorRepository constructorRepository,
                            ParameterRepository parameterRepository,
                            ClassRelationRepository classRelationRepository) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.classRelationRepository = classRelationRepository;
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

        Map<UUID, List<Field>> fieldsByClass = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<String>> paramTypesByMethod = RubricParameterMaps.byMethod(methodParams);
        Map<UUID, List<String>> paramTypesByConstructor = RubricParameterMaps.byConstructor(constructorParams);

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
                        paramTypesByConstructor));
            }
            List<RelationRubric> relationRubrics = relationsByChallenge.getOrDefault(challenge.getId(), List.of())
                    .stream()
                    .map(this::toRelationRubric)
                    .toList();
            byNumber.put(challenge.getChallengeNumber(),
                    new ChallengeRubric(challenge.getId(), challenge.getChallengeNumber(), challenge.getName(),
                            classRubrics, relationRubrics));
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
                constructorRubrics);
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
}
