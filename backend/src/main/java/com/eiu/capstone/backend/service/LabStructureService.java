package com.eiu.capstone.backend.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ClassRelation;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.ConstructorDeclaration;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.FieldDeclaration;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;
import com.eiu.capstone.backend.model.Parameter;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorDeclarationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.TermRepository;

@Service
public class LabStructureService {

    private final LabRepository labRepository;
    private final TermRepository termRepository;
    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassRelationRepository classRelationRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final FieldDeclarationRepository fieldDeclarationRepository;
    private final MethodDeclarationRepository methodDeclarationRepository;
    private final ConstructorDeclarationRepository constructorDeclarationRepository;
    private final MasterDataRepository masterDataRepository;
    private final RubricCacheInvalidationSupport rubricCacheInvalidationSupport;

    public LabStructureService(LabRepository labRepository,
                               TermRepository termRepository,
                               ChallengeRepository challengeRepository,
                               ClassEntityRepository classEntityRepository,
                               ClassRelationRepository classRelationRepository,
                               FieldRepository fieldRepository,
                               MethodRepository methodRepository,
                               ConstructorRepository constructorRepository,
                               ParameterRepository parameterRepository,
                               FieldDeclarationRepository fieldDeclarationRepository,
                               MethodDeclarationRepository methodDeclarationRepository,
                               ConstructorDeclarationRepository constructorDeclarationRepository,
                               MasterDataRepository masterDataRepository,
                               RubricCacheInvalidationSupport rubricCacheInvalidationSupport) {
        this.labRepository = labRepository;
        this.termRepository = termRepository;
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.classRelationRepository = classRelationRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.fieldDeclarationRepository = fieldDeclarationRepository;
        this.methodDeclarationRepository = methodDeclarationRepository;
        this.constructorDeclarationRepository = constructorDeclarationRepository;
        this.masterDataRepository = masterDataRepository;
        this.rubricCacheInvalidationSupport = rubricCacheInvalidationSupport;
    }

    @Transactional(readOnly = true)
    public LabStructureResponse loadForEditor(UUID labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        if (challenges.isEmpty()) {
            return new LabStructureResponse(lab.getId(), lab.getName(), lab.getTerm().getId(), List.of());
        }

        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        List<Field> fields = classes.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> methods = classes.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Constructor> constructors = classes.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<Parameter> methodParams = methods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(methods);
        List<Parameter> constructorParams = constructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(constructors);
        List<ClassRelation> allRelations = classes.isEmpty() ? List.of()
                : classRelationRepository.findByClassEntityInWithEndpoints(classes);

        Map<UUID, List<ClassEntity>> classesByChallenge = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        Map<UUID, List<Field>> fieldsByClass = fields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = methods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = constructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<Parameter>> paramsByMethod = methodParams.stream()
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        Map<UUID, List<Parameter>> paramsByConstructor = constructorParams.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));
        Map<UUID, List<ClassRelation>> relationsByChallenge = allRelations.stream()
                .collect(Collectors.groupingBy(r -> r.getClassEntity().getChallenge().getId()));

        List<ChallengeStructureDTO> challengeDtos = challenges.stream()
                .map(challenge -> toChallengeDto(
                        challenge,
                        classesByChallenge.getOrDefault(challenge.getId(), List.of()),
                        fieldsByClass,
                        methodsByClass,
                        constructorsByClass,
                        paramsByMethod,
                        paramsByConstructor,
                        relationsByChallenge.getOrDefault(challenge.getId(), List.of())))
                .toList();

        return new LabStructureResponse(lab.getId(), lab.getName(), lab.getTerm().getId(), challengeDtos);
    }

    @Transactional
    public LabStructureResponse saveLabStructure(UUID labId, LabStructureResponse payload) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (!Objects.equals(labId, payload.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab id mismatch");
        }

        List<Challenge> existingChallenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        Set<UUID> keptChallengeIds = new HashSet<>();

        int nextChallengeNumber = existingChallenges.stream()
                .map(Challenge::getChallengeNumber)
                .max(Integer::compareTo)
                .orElse(0);

        List<ChallengeStructureDTO> challengePayloads = payload.challenges() != null ? payload.challenges() : List.of();
        for (ChallengeStructureDTO challengeDto : challengePayloads) {
            Challenge challenge = upsertChallenge(lab, challengeDto, nextChallengeNumber, keptChallengeIds);
            if (challengeDto.challengeNumber() == null && challengeDto.id() == null) {
                nextChallengeNumber = challenge.getChallengeNumber();
            }
            syncClasses(challenge, challengeDto.classes());
            syncRelations(challenge, challengeDto.relations());
        }

        for (Challenge existing : existingChallenges) {
            if (!keptChallengeIds.contains(existing.getId())) {
                deleteChallengeCascade(existing);
            }
        }

        if (payload.name() != null && !payload.name().isBlank()) {
            lab.setName(payload.name().trim());
            labRepository.save(lab);
        }

        rubricCacheInvalidationSupport.invalidateLab(labId);
        return loadForEditor(labId);
    }

    @Transactional
    public LabStructureResponse createLab(CreateLabRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab name is required");
        }
        if (request.termId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "termId is required");
        }
        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid termId"));
        Lab lab = new Lab();
        lab.setName(request.name().trim());
        lab.setTerm(term);
        lab = labRepository.save(lab);
        return new LabStructureResponse(lab.getId(), lab.getName(), term.getId(), List.of());
    }

    @Transactional
    public void deleteLabCascade(UUID labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        for (Challenge challenge : challenges) {
            deleteChallengeCascade(challenge);
        }
        labRepository.delete(lab);
        rubricCacheInvalidationSupport.invalidateLab(labId);
    }

    private Challenge upsertChallenge(Lab lab, ChallengeStructureDTO dto, int nextChallengeNumber, Set<UUID> keptChallengeIds) {
        Challenge challenge;
        if (dto.id() != null) {
            challenge = challengeRepository.findById(dto.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown challenge id: " + dto.id()));
            if (!challenge.getLab().getId().equals(lab.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge does not belong to lab");
            }
        } else {
            challenge = new Challenge();
            challenge.setLab(lab);
        }
        challenge.setName(requireNonBlank(dto.name(), "Challenge name"));
        if (dto.challengeNumber() != null) {
            challenge.setChallengeNumber(dto.challengeNumber());
        } else if (dto.id() == null) {
            challenge.setChallengeNumber(nextChallengeNumber + 1);
        }
        challenge = challengeRepository.save(challenge);
        keptChallengeIds.add(challenge.getId());
        return challenge;
    }

    private void syncClasses(Challenge challenge, List<ClassStructureDTO> classDtos) {
        List<ClassEntity> existingClasses = classEntityRepository.findByChallenge_Id(challenge.getId());
        Set<UUID> keptClassIds = new HashSet<>();
        List<ClassStructureDTO> payloads = classDtos != null ? classDtos : List.of();

        for (ClassStructureDTO classDto : payloads) {
            ClassEntity classEntity = upsertClass(challenge, classDto);
            keptClassIds.add(classEntity.getId());
            syncFields(classEntity, classDto.fields());
            syncMethods(classEntity, classDto.methods());
            syncConstructors(classEntity, classDto.constructors());
        }

        for (ClassEntity existing : existingClasses) {
            if (!keptClassIds.contains(existing.getId())) {
                deleteRelationsForClass(existing.getId());
                deleteClassCascade(existing);
            }
        }
    }

    private void syncRelations(Challenge challenge, List<RelationStructureDTO> relationDtos) {
        List<ClassEntity> challengeClasses = classEntityRepository.findByChallenge_Id(challenge.getId());
        Set<UUID> classIds = challengeClasses.stream().map(ClassEntity::getId).collect(Collectors.toSet());
        List<ClassRelation> existing = challengeClasses.isEmpty() ? List.of()
                : classRelationRepository.findByClassEntityInWithEndpoints(challengeClasses);
        Set<UUID> kept = new HashSet<>();

        for (RelationStructureDTO dto : relationDtos != null ? relationDtos : List.<RelationStructureDTO>of()) {
            if (dto.sourceClassId() == null || dto.targetClassId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relation source and target class are required");
            }
            if (!classIds.contains(dto.sourceClassId()) || !classIds.contains(dto.targetClassId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relation classes must belong to the same problem");
            }
            if (dto.sourceClassId().equals(dto.targetClassId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Relation source and target must differ");
            }
            ClassEntity source = classEntityRepository.findById(dto.sourceClassId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown source class"));
            ClassEntity target = classEntityRepository.findById(dto.targetClassId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown target class"));

            ClassRelation relation;
            if (dto.id() != null) {
                relation = classRelationRepository.findById(dto.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown relation id"));
            } else {
                relation = new ClassRelation();
            }
            relation.setClassEntity(source);
            relation.setTargetClassEntity(target);
            relation.setRelationType(resolveMasterData(dto.relationTypeId(), "relation type"));
            relation = classRelationRepository.save(relation);
            kept.add(relation.getId());
        }

        for (ClassRelation row : existing) {
            if (!kept.contains(row.getId())) {
                classRelationRepository.delete(row);
            }
        }
    }

    private void deleteRelationsForClass(UUID classId) {
        List<ClassRelation> asSource = classRelationRepository.findByClassEntity_Id(classId);
        List<ClassRelation> asTarget = classRelationRepository.findByTargetClassEntity_Id(classId);
        Set<UUID> deleted = new HashSet<>();
        for (ClassRelation relation : asSource) {
            if (deleted.add(relation.getId())) {
                classRelationRepository.delete(relation);
            }
        }
        for (ClassRelation relation : asTarget) {
            if (deleted.add(relation.getId())) {
                classRelationRepository.delete(relation);
            }
        }
    }

    private ClassEntity upsertClass(Challenge challenge, ClassStructureDTO dto) {
        ClassEntity classEntity;
        if (dto.id() != null) {
            classEntity = classEntityRepository.findById(dto.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown class id: " + dto.id()));
        } else {
            classEntity = new ClassEntity();
            classEntity.setChallenge(challenge);
        }
        classEntity.setName(requireNonBlank(dto.name(), "Class name"));
        classEntity.setScope(resolveMasterData(dto.scopeId(), "scope"));
        classEntity.setDeclaringType(resolveMasterData(dto.declaringTypeId(), "declaring type"));
        classEntity.setAbstract(dto.isAbstract());
        return classEntityRepository.save(classEntity);
    }

    private void syncFields(ClassEntity classEntity, List<FieldStructureDTO> fieldDtos) {
        List<Field> existing = fieldRepository.findByClassEntity_Id(classEntity.getId());
        Set<UUID> kept = new HashSet<>();
        for (FieldStructureDTO dto : fieldDtos != null ? fieldDtos : List.<FieldStructureDTO>of()) {
            Field field;
            FieldDeclaration declaration;
            if (dto.id() != null) {
                field = fieldRepository.findById(dto.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown field id"));
                declaration = field.getFieldDeclaration();
            } else {
                field = new Field();
                field.setClassEntity(classEntity);
                declaration = new FieldDeclaration();
            }
            declaration.setName(requireNonBlank(dto.name(), "Field name"));
            declaration.setDataType(requireNonBlank(dto.dataType(), "Field type"));
            declaration.setScope(resolveMasterData(dto.scopeId(), "field scope"));
            declaration = fieldDeclarationRepository.save(declaration);
            field.setFieldDeclaration(declaration);
            field.setName(declaration.getName());
            field = fieldRepository.save(field);
            kept.add(field.getId());
        }
        for (Field row : existing) {
            if (!kept.contains(row.getId())) {
                deleteField(row);
            }
        }
    }

    private void syncMethods(ClassEntity classEntity, List<MethodStructureDTO> methodDtos) {
        List<Method> existing = methodRepository.findByClassEntity_Id(classEntity.getId());
        Set<UUID> kept = new HashSet<>();
        for (MethodStructureDTO dto : methodDtos != null ? methodDtos : List.<MethodStructureDTO>of()) {
            Method method;
            MethodDeclaration declaration;
            if (dto.id() != null) {
                method = methodRepository.findById(dto.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown method id"));
                declaration = method.getMethodDeclaration();
            } else {
                method = new Method();
                method.setClassEntity(classEntity);
                declaration = new MethodDeclaration();
            }
            declaration.setName(requireNonBlank(dto.name(), "Method name"));
            declaration.setReturnType(requireNonBlank(dto.returnType(), "Return type"));
            declaration.setScope(resolveMasterData(dto.scopeId(), "method scope"));
            declaration.setStatic(dto.isStatic());
            declaration.setAbstract(dto.isAbstract());
            declaration.setFinal(false);
            declaration = methodDeclarationRepository.save(declaration);
            method.setMethodDeclaration(declaration);
            method.setName(declaration.getName());
            method = methodRepository.save(method);
            syncMethodParameters(method, dto.parameters());
            kept.add(method.getId());
        }
        for (Method row : existing) {
            if (!kept.contains(row.getId())) {
                deleteMethod(row);
            }
        }
    }

    private void syncConstructors(ClassEntity classEntity, List<ConstructorStructureDTO> constructorDtos) {
        List<Constructor> existing = constructorRepository.findByClassEntity_Id(classEntity.getId());
        Set<UUID> kept = new HashSet<>();
        for (ConstructorStructureDTO dto : constructorDtos != null ? constructorDtos : List.<ConstructorStructureDTO>of()) {
            Constructor constructor;
            ConstructorDeclaration declaration;
            if (dto.id() != null) {
                constructor = constructorRepository.findById(dto.id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown constructor id"));
                declaration = constructor.getConstructorDeclaration();
            } else {
                constructor = new Constructor();
                constructor.setClassEntity(classEntity);
                declaration = new ConstructorDeclaration();
            }
            String constructorName = dto.name() != null && !dto.name().isBlank() ? dto.name().trim() : classEntity.getName();
            declaration.setName(constructorName);
            declaration.setScope(resolveMasterData(dto.scopeId(), "constructor scope"));
            declaration.setDefault(dto.isDefault());
            declaration = constructorDeclarationRepository.save(declaration);
            constructor.setConstructorDeclaration(declaration);
            constructor.setName(constructorName);
            constructor = constructorRepository.save(constructor);
            syncConstructorParameters(constructor, dto.parameters());
            kept.add(constructor.getId());
        }
        for (Constructor row : existing) {
            if (!kept.contains(row.getId())) {
                deleteConstructor(row);
            }
        }
    }

    private void syncMethodParameters(Method method, List<ParameterStructureDTO> parameterDtos) {
        List<Parameter> existing = parameterRepository.findByMethod_IdOrderByOrderIndexAsc(method.getId());
        parameterRepository.deleteAll(existing);
        parameterRepository.flush();
        saveParameters(parameterDtos, method, null);
    }

    private void syncConstructorParameters(Constructor constructor, List<ParameterStructureDTO> parameterDtos) {
        List<Parameter> existing = parameterRepository.findByConstructorEntity_IdOrderByOrderIndexAsc(constructor.getId());
        parameterRepository.deleteAll(existing);
        parameterRepository.flush();
        saveParameters(parameterDtos, null, constructor);
    }

    private void saveParameters(List<ParameterStructureDTO> parameterDtos, Method method, Constructor constructor) {
        if (parameterDtos == null) {
            return;
        }
        int index = 0;
        for (ParameterStructureDTO dto : parameterDtos) {
            Parameter parameter = new Parameter();
            parameter.setMethod(method);
            parameter.setConstructorEntity(constructor);
            parameter.setName(requireNonBlank(dto.name(), "Parameter name"));
            parameter.setDataType(requireNonBlank(dto.dataType(), "Parameter type"));
            parameter.setOrderIndex(dto.orderIndex() >= 0 ? dto.orderIndex() : index);
            parameter.setFinal(dto.isFinal());
            parameterRepository.save(parameter);
            index++;
        }
    }

    private void deleteChallengeCascade(Challenge challenge) {
        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challenge.getId());
        for (ClassEntity classEntity : classes) {
            deleteClassCascade(classEntity);
        }
        challengeRepository.delete(challenge);
    }

    private void deleteClassCascade(ClassEntity classEntity) {
        deleteRelationsForClass(classEntity.getId());
        for (Field field : fieldRepository.findByClassEntity_Id(classEntity.getId())) {
            deleteField(field);
        }
        for (Method method : methodRepository.findByClassEntity_Id(classEntity.getId())) {
            deleteMethod(method);
        }
        for (Constructor constructor : constructorRepository.findByClassEntity_Id(classEntity.getId())) {
            deleteConstructor(constructor);
        }
        classEntityRepository.delete(classEntity);
    }

    private void deleteField(Field field) {
        UUID declarationId = field.getFieldDeclaration().getId();
        fieldRepository.delete(field);
        fieldDeclarationRepository.deleteById(declarationId);
    }

    private void deleteMethod(Method method) {
        parameterRepository.deleteAll(parameterRepository.findByMethod_IdOrderByOrderIndexAsc(method.getId()));
        UUID declarationId = method.getMethodDeclaration().getId();
        methodRepository.delete(method);
        methodDeclarationRepository.deleteById(declarationId);
    }

    private void deleteConstructor(Constructor constructor) {
        parameterRepository.deleteAll(parameterRepository.findByConstructorEntity_IdOrderByOrderIndexAsc(constructor.getId()));
        UUID declarationId = constructor.getConstructorDeclaration().getId();
        constructorRepository.delete(constructor);
        constructorDeclarationRepository.deleteById(declarationId);
    }

    private MasterData resolveMasterData(Integer id, String label) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing " + label + " id");
        }
        return masterDataRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + label + " id: " + id));
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        return value.trim();
    }

    private ChallengeStructureDTO toChallengeDto(Challenge challenge,
                                                 List<ClassEntity> classes,
                                                 Map<UUID, List<Field>> fieldsByClass,
                                                 Map<UUID, List<Method>> methodsByClass,
                                                 Map<UUID, List<Constructor>> constructorsByClass,
                                                 Map<UUID, List<Parameter>> paramsByMethod,
                                                 Map<UUID, List<Parameter>> paramsByConstructor,
                                                 List<ClassRelation> relations) {
        List<ClassStructureDTO> classDtos = classes.stream()
                .sorted(Comparator.comparing(ClassEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(classEntity -> toClassDto(
                        classEntity,
                        fieldsByClass.getOrDefault(classEntity.getId(), List.of()),
                        methodsByClass.getOrDefault(classEntity.getId(), List.of()),
                        constructorsByClass.getOrDefault(classEntity.getId(), List.of()),
                        paramsByMethod,
                        paramsByConstructor))
                .toList();
        List<RelationStructureDTO> relationDtos = relations.stream()
                .map(r -> new RelationStructureDTO(
                        r.getId(),
                        r.getClassEntity().getId(),
                        r.getTargetClassEntity().getId(),
                        r.getRelationType().getId()))
                .toList();
        return new ChallengeStructureDTO(
                challenge.getId(),
                challenge.getName(),
                challenge.getChallengeNumber(),
                classDtos,
                relationDtos);
    }

    private ClassStructureDTO toClassDto(ClassEntity classEntity,
                                         List<Field> fields,
                                         List<Method> methods,
                                         List<Constructor> constructors,
                                         Map<UUID, List<Parameter>> paramsByMethod,
                                         Map<UUID, List<Parameter>> paramsByConstructor) {
        List<FieldStructureDTO> fieldDtos = fields.stream()
                .map(f -> new FieldStructureDTO(
                        f.getId(),
                        f.getName(),
                        f.getFieldDeclaration().getDataType(),
                        f.getFieldDeclaration().getScope().getId()))
                .toList();
        List<MethodStructureDTO> methodDtos = methods.stream()
                .map(m -> new MethodStructureDTO(
                        m.getId(),
                        m.getName(),
                        m.getMethodDeclaration().getReturnType(),
                        m.getMethodDeclaration().getScope().getId(),
                        m.getMethodDeclaration().isStatic(),
                        m.getMethodDeclaration().isAbstract(),
                        mapParameters(paramsByMethod.getOrDefault(m.getId(), List.of()))))
                .toList();
        List<ConstructorStructureDTO> constructorDtos = constructors.stream()
                .map(c -> new ConstructorStructureDTO(
                        c.getId(),
                        c.getName(),
                        c.getConstructorDeclaration().getScope().getId(),
                        c.getConstructorDeclaration().isDefault(),
                        mapParameters(paramsByConstructor.getOrDefault(c.getId(), List.of()))))
                .toList();
        return new ClassStructureDTO(
                classEntity.getId(),
                classEntity.getName(),
                classEntity.getScope().getId(),
                classEntity.getDeclaringType().getId(),
                classEntity.isAbstract(),
                fieldDtos,
                methodDtos,
                constructorDtos);
    }

    private List<ParameterStructureDTO> mapParameters(List<Parameter> parameters) {
        return parameters.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(p -> new ParameterStructureDTO(p.getId(), p.getName(), p.getDataType(), p.getOrderIndex(), p.isFinal()))
                .toList();
    }
}
