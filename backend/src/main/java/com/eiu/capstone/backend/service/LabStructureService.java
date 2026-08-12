package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
        return buildLabStructureResponse(lab);
    }

    private LabStructureResponse buildLabStructureResponse(Lab lab) {
        UUID labId = lab.getId();
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

        List<ChallengeStructureDTO> challengePayloads = payload.challenges() != null ? payload.challenges() : List.of();
        SaveContext ctx = loadSaveContext(labId);
        Set<UUID> keptChallengeIds = new HashSet<>();

        Set<UUID> payloadChallengeIds = challengePayloads.stream()
                .map(ChallengeStructureDTO::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Challenge existing : List.copyOf(ctx.challengesById.values())) {
            if (!payloadChallengeIds.contains(existing.getId())) {
                deleteChallengeCascade(ctx, existing);
            }
        }
        challengeRepository.flush();

        Set<Integer> usedChallengeNumbers = ctx.challengesById.values().stream()
                .map(Challenge::getChallengeNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        for (ChallengeStructureDTO challengeDto : challengePayloads) {
            Challenge challenge = upsertChallenge(ctx, lab, challengeDto, usedChallengeNumbers, keptChallengeIds);
            syncClasses(ctx, challenge, challengeDto.classes());
            syncRelations(ctx, challenge, challengeDto.relations());
        }

        if (payload.name() != null && !payload.name().isBlank()) {
            lab.setName(payload.name().trim());
            labRepository.save(lab);
        }

        rubricCacheInvalidationSupport.invalidateLab(labId);
        String labName = payload.name() != null && !payload.name().isBlank() ? payload.name().trim() : lab.getName();
        List<ChallengeStructureDTO> savedChallenges = challengePayloads.stream()
                .map(dto -> {
                    Challenge saved = dto.id() != null ? ctx.challengesById.get(dto.id()) : null;
                    if (saved == null) {
                        return dto;
                    }
                    return new ChallengeStructureDTO(
                            saved.getId(),
                            saved.getName(),
                            saved.getChallengeNumber(),
                            dto.classes(),
                            dto.relations());
                })
                .toList();
        return new LabStructureResponse(labId, labName, lab.getTerm().getId(), savedChallenges);
    }

    private SaveContext loadSaveContext(UUID labId) {
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        Map<UUID, Challenge> challengesById = challenges.stream()
                .collect(Collectors.toMap(Challenge::getId, Function.identity()));

        List<ClassEntity> classes = challenges.isEmpty() ? List.of()
                : classEntityRepository.findByChallengeInWithAttributes(challenges);
        Map<UUID, ClassEntity> classesById = classes.stream()
                .collect(Collectors.toMap(ClassEntity::getId, Function.identity()));
        Map<UUID, List<ClassEntity>> classesByChallengeId = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));

        List<Field> fields = classes.isEmpty() ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(classes);
        Map<UUID, Field> fieldsById = fields.stream()
                .collect(Collectors.toMap(Field::getId, Function.identity()));
        Map<UUID, List<Field>> fieldsByClassId = fields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));

        List<Method> methods = classes.isEmpty() ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(classes);
        Map<UUID, Method> methodsById = methods.stream()
                .collect(Collectors.toMap(Method::getId, Function.identity()));
        Map<UUID, List<Method>> methodsByClassId = methods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));

        List<Constructor> constructors = classes.isEmpty() ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(classes);
        Map<UUID, Constructor> constructorsById = constructors.stream()
                .collect(Collectors.toMap(Constructor::getId, Function.identity()));
        Map<UUID, List<Constructor>> constructorsByClassId = constructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));

        List<ClassRelation> relations = classes.isEmpty() ? List.of()
                : classRelationRepository.findByClassEntityInWithEndpoints(classes);
        Map<UUID, ClassRelation> relationsById = relations.stream()
                .collect(Collectors.toMap(ClassRelation::getId, Function.identity()));
        Map<UUID, List<ClassRelation>> relationsByChallengeId = relations.stream()
                .collect(Collectors.groupingBy(r -> r.getClassEntity().getChallenge().getId()));

        Map<Integer, MasterData> masterDataById = masterDataRepository.findAll().stream()
                .collect(Collectors.toMap(MasterData::getId, Function.identity()));

        return new SaveContext(
                challengesById,
                classesById,
                classesByChallengeId,
                fieldsById,
                fieldsByClassId,
                methodsById,
                methodsByClassId,
                constructorsById,
                constructorsByClassId,
                relationsById,
                relationsByChallengeId,
                masterDataById);
    }

    private static final class SaveContext {
        final Map<UUID, Challenge> challengesById;
        final Map<UUID, ClassEntity> classesById;
        final Map<UUID, List<ClassEntity>> classesByChallengeId;
        final Map<UUID, Field> fieldsById;
        final Map<UUID, List<Field>> fieldsByClassId;
        final Map<UUID, Method> methodsById;
        final Map<UUID, List<Method>> methodsByClassId;
        final Map<UUID, Constructor> constructorsById;
        final Map<UUID, List<Constructor>> constructorsByClassId;
        final Map<UUID, ClassRelation> relationsById;
        final Map<UUID, List<ClassRelation>> relationsByChallengeId;
        final Map<Integer, MasterData> masterDataById;

        SaveContext(Map<UUID, Challenge> challengesById,
                    Map<UUID, ClassEntity> classesById,
                    Map<UUID, List<ClassEntity>> classesByChallengeId,
                    Map<UUID, Field> fieldsById,
                    Map<UUID, List<Field>> fieldsByClassId,
                    Map<UUID, Method> methodsById,
                    Map<UUID, List<Method>> methodsByClassId,
                    Map<UUID, Constructor> constructorsById,
                    Map<UUID, List<Constructor>> constructorsByClassId,
                    Map<UUID, ClassRelation> relationsById,
                    Map<UUID, List<ClassRelation>> relationsByChallengeId,
                    Map<Integer, MasterData> masterDataById) {
            this.challengesById = new HashMap<>(challengesById);
            this.classesById = new HashMap<>(classesById);
            this.classesByChallengeId = new HashMap<>(classesByChallengeId);
            this.fieldsById = new HashMap<>(fieldsById);
            this.fieldsByClassId = new HashMap<>(fieldsByClassId);
            this.methodsById = new HashMap<>(methodsById);
            this.methodsByClassId = new HashMap<>(methodsByClassId);
            this.constructorsById = new HashMap<>(constructorsById);
            this.constructorsByClassId = new HashMap<>(constructorsByClassId);
            this.relationsById = new HashMap<>(relationsById);
            this.relationsByChallengeId = new HashMap<>(relationsByChallengeId);
            this.masterDataById = masterDataById;
        }

        void putChallenge(Challenge challenge) {
            challengesById.put(challenge.getId(), challenge);
        }

        void putClass(ClassEntity classEntity) {
            classesById.put(classEntity.getId(), classEntity);
            UUID challengeId = classEntity.getChallenge().getId();
            classesByChallengeId.computeIfAbsent(challengeId, ignored -> new ArrayList<>());
            List<ClassEntity> bucket = classesByChallengeId.get(challengeId);
            if (bucket.stream().noneMatch(c -> c.getId().equals(classEntity.getId()))) {
                bucket.add(classEntity);
            }
        }

        void putField(Field field) {
            fieldsById.put(field.getId(), field);
            UUID classId = field.getClassEntity().getId();
            fieldsByClassId.computeIfAbsent(classId, ignored -> new ArrayList<>());
            List<Field> bucket = fieldsByClassId.get(classId);
            if (bucket.stream().noneMatch(f -> f.getId().equals(field.getId()))) {
                bucket.add(field);
            }
        }

        void removeField(UUID fieldId, UUID classId) {
            fieldsById.remove(fieldId);
            List<Field> bucket = fieldsByClassId.get(classId);
            if (bucket != null) {
                bucket.removeIf(f -> f.getId().equals(fieldId));
            }
        }

        void putMethod(Method method) {
            methodsById.put(method.getId(), method);
            UUID classId = method.getClassEntity().getId();
            methodsByClassId.computeIfAbsent(classId, ignored -> new ArrayList<>());
            List<Method> bucket = methodsByClassId.get(classId);
            if (bucket.stream().noneMatch(m -> m.getId().equals(method.getId()))) {
                bucket.add(method);
            }
        }

        void removeMethod(UUID methodId, UUID classId) {
            methodsById.remove(methodId);
            List<Method> bucket = methodsByClassId.get(classId);
            if (bucket != null) {
                bucket.removeIf(m -> m.getId().equals(methodId));
            }
        }

        void putConstructor(Constructor constructor) {
            constructorsById.put(constructor.getId(), constructor);
            UUID classId = constructor.getClassEntity().getId();
            constructorsByClassId.computeIfAbsent(classId, ignored -> new ArrayList<>());
            List<Constructor> bucket = constructorsByClassId.get(classId);
            if (bucket.stream().noneMatch(c -> c.getId().equals(constructor.getId()))) {
                bucket.add(constructor);
            }
        }

        void removeConstructor(UUID constructorId, UUID classId) {
            constructorsById.remove(constructorId);
            List<Constructor> bucket = constructorsByClassId.get(classId);
            if (bucket != null) {
                bucket.removeIf(c -> c.getId().equals(constructorId));
            }
        }

        void putRelation(ClassRelation relation) {
            relationsById.put(relation.getId(), relation);
            UUID challengeId = relation.getClassEntity().getChallenge().getId();
            relationsByChallengeId.computeIfAbsent(challengeId, ignored -> new ArrayList<>());
            List<ClassRelation> bucket = relationsByChallengeId.get(challengeId);
            if (bucket.stream().noneMatch(r -> r.getId().equals(relation.getId()))) {
                bucket.add(relation);
            }
        }

        void removeRelation(UUID relationId, UUID challengeId) {
            relationsById.remove(relationId);
            List<ClassRelation> bucket = relationsByChallengeId.get(challengeId);
            if (bucket != null) {
                bucket.removeIf(r -> r.getId().equals(relationId));
            }
        }

        void removeClass(UUID classId, UUID challengeId) {
            classesById.remove(classId);
            List<ClassEntity> bucket = classesByChallengeId.get(challengeId);
            if (bucket != null) {
                bucket.removeIf(c -> c.getId().equals(classId));
            }
        }

        void removeChallenge(UUID challengeId) {
            challengesById.remove(challengeId);
            classesByChallengeId.remove(challengeId);
            relationsByChallengeId.remove(challengeId);
        }
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
        SaveContext ctx = loadSaveContext(labId);
        for (Challenge challenge : List.copyOf(ctx.challengesById.values())) {
            deleteChallengeCascade(ctx, challenge);
        }
        labRepository.delete(lab);
        rubricCacheInvalidationSupport.invalidateLab(labId);
    }

    private Challenge upsertChallenge(SaveContext ctx, Lab lab, ChallengeStructureDTO dto,
                                        Set<Integer> usedChallengeNumbers, Set<UUID> keptChallengeIds) {
        Challenge challenge;
        boolean isNew;
        if (dto.id() != null) {
            challenge = ctx.challengesById.get(dto.id());
            if (challenge == null) {
                challenge = new Challenge();
                challenge.setId(dto.id());
                challenge.setLab(lab);
                isNew = true;
            } else {
                if (challenge.getLab() != null && !challenge.getLab().getId().equals(lab.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge does not belong to lab");
                }
                if (challenge.getLab() == null) {
                    challenge.setLab(lab);
                }
                isNew = false;
            }
        } else {
            challenge = new Challenge();
            challenge.setLab(lab);
            isNew = true;
        }
        challenge.setName(requireNonBlank(dto.name(), "Challenge name"));
        if (isNew) {
            challenge.setChallengeNumber(allocateChallengeNumber(usedChallengeNumbers, dto.challengeNumber()));
        } else {
            Integer currentNumber = challenge.getChallengeNumber();
            Integer requestedNumber = dto.challengeNumber();
            if (requestedNumber != null && !requestedNumber.equals(currentNumber)) {
                if (usedChallengeNumbers.contains(requestedNumber)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Challenge number " + requestedNumber + " is already in use");
                }
                if (currentNumber != null) {
                    usedChallengeNumbers.remove(currentNumber);
                }
                challenge.setChallengeNumber(requestedNumber);
                usedChallengeNumbers.add(requestedNumber);
            } else if (currentNumber != null) {
                usedChallengeNumbers.add(currentNumber);
            }
        }
        challenge = challengeRepository.save(challenge);
        ctx.putChallenge(challenge);
        keptChallengeIds.add(challenge.getId());
        return challenge;
    }

    private int allocateChallengeNumber(Set<Integer> usedChallengeNumbers, Integer preferred) {
        if (preferred != null && !usedChallengeNumbers.contains(preferred)) {
            usedChallengeNumbers.add(preferred);
            return preferred;
        }
        int number = usedChallengeNumbers.stream().max(Integer::compareTo).orElse(0) + 1;
        while (usedChallengeNumbers.contains(number)) {
            number++;
        }
        usedChallengeNumbers.add(number);
        return number;
    }

    private void syncClasses(SaveContext ctx, Challenge challenge, List<ClassStructureDTO> classDtos) {
        List<ClassEntity> existingClasses = List.copyOf(ctx.classesByChallengeId.getOrDefault(challenge.getId(), List.of()));
        Set<UUID> keptClassIds = new HashSet<>();
        List<ClassStructureDTO> payloads = classDtos != null ? classDtos : List.of();

        for (ClassStructureDTO classDto : payloads) {
            ClassEntity classEntity = upsertClass(ctx, challenge, classDto);
            keptClassIds.add(classEntity.getId());
            syncFields(ctx, classEntity, classDto.fields());
            syncMethods(ctx, classEntity, classDto.methods());
            syncConstructors(ctx, classEntity, classDto.constructors());
        }

        for (ClassEntity existing : existingClasses) {
            if (!keptClassIds.contains(existing.getId())) {
                deleteRelationsForClass(ctx, existing.getId());
                deleteClassCascade(ctx, existing);
            }
        }
    }

    private void syncRelations(SaveContext ctx, Challenge challenge, List<RelationStructureDTO> relationDtos) {
        List<ClassEntity> challengeClasses = ctx.classesByChallengeId.getOrDefault(challenge.getId(), List.of());
        Set<UUID> classIds = challengeClasses.stream().map(ClassEntity::getId).collect(Collectors.toSet());
        List<ClassRelation> existing = List.copyOf(ctx.relationsByChallengeId.getOrDefault(challenge.getId(), List.of()));
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
            ClassEntity source = ctx.classesById.get(dto.sourceClassId());
            ClassEntity target = ctx.classesById.get(dto.targetClassId());
            if (source == null || target == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown relation class");
            }

            ClassRelation relation;
            if (dto.id() != null) {
                relation = ctx.relationsById.get(dto.id());
                if (relation == null) {
                    relation = new ClassRelation();
                    relation.setId(dto.id());
                }
            } else {
                relation = new ClassRelation();
            }
            relation.setClassEntity(source);
            relation.setTargetClassEntity(target);
            relation.setRelationType(resolveMasterData(ctx, dto.relationTypeId(), "relation type"));
            relation = classRelationRepository.save(relation);
            ctx.putRelation(relation);
            kept.add(relation.getId());
        }

        for (ClassRelation row : existing) {
            if (!kept.contains(row.getId())) {
                classRelationRepository.delete(row);
                ctx.removeRelation(row.getId(), challenge.getId());
            }
        }
    }

    private void deleteRelationsForClass(SaveContext ctx, UUID classId) {
        List<ClassRelation> asSource = classRelationRepository.findByClassEntity_Id(classId);
        List<ClassRelation> asTarget = classRelationRepository.findByTargetClassEntity_Id(classId);
        Set<UUID> deleted = new HashSet<>();
        for (ClassRelation relation : asSource) {
            if (deleted.add(relation.getId())) {
                classRelationRepository.delete(relation);
                ctx.removeRelation(relation.getId(), relation.getClassEntity().getChallenge().getId());
            }
        }
        for (ClassRelation relation : asTarget) {
            if (deleted.add(relation.getId())) {
                classRelationRepository.delete(relation);
                ctx.removeRelation(relation.getId(), relation.getClassEntity().getChallenge().getId());
            }
        }
    }

    private ClassEntity upsertClass(SaveContext ctx, Challenge challenge, ClassStructureDTO dto) {
        ClassEntity classEntity;
        if (dto.id() != null) {
            classEntity = ctx.classesById.get(dto.id());
            if (classEntity == null) {
                classEntity = new ClassEntity();
                classEntity.setId(dto.id());
                classEntity.setChallenge(challenge);
            } else if (classEntity.getChallenge() != null
                    && !classEntity.getChallenge().getId().equals(challenge.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class does not belong to problem");
            } else if (classEntity.getChallenge() == null) {
                classEntity.setChallenge(challenge);
            }
        } else {
            classEntity = new ClassEntity();
            classEntity.setChallenge(challenge);
        }
        classEntity.setName(requireNonBlank(dto.name(), "Class name"));
        classEntity.setScope(resolveMasterData(ctx, dto.scopeId(), "scope"));
        classEntity.setDeclaringType(resolveMasterData(ctx, dto.declaringTypeId(), "declaring type"));
        classEntity.setAbstract(dto.isAbstract());
        classEntity = classEntityRepository.save(classEntity);
        ctx.putClass(classEntity);
        return classEntity;
    }

    private void syncFields(SaveContext ctx, ClassEntity classEntity, List<FieldStructureDTO> fieldDtos) {
        List<Field> existing = List.copyOf(ctx.fieldsByClassId.getOrDefault(classEntity.getId(), List.of()));
        Set<UUID> kept = new HashSet<>();
        for (FieldStructureDTO dto : fieldDtos != null ? fieldDtos : List.<FieldStructureDTO>of()) {
            Field field;
            FieldDeclaration declaration;
            if (dto.id() != null) {
                field = ctx.fieldsById.get(dto.id());
                if (field == null) {
                    field = new Field();
                    field.setId(dto.id());
                    field.setClassEntity(classEntity);
                    declaration = new FieldDeclaration();
                } else {
                    if (field.getClassEntity() != null
                            && !field.getClassEntity().getId().equals(classEntity.getId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field does not belong to class");
                    }
                    if (field.getClassEntity() == null) {
                        field.setClassEntity(classEntity);
                    }
                    declaration = field.getFieldDeclaration();
                    if (declaration == null) {
                        declaration = new FieldDeclaration();
                    }
                }
            } else {
                field = new Field();
                field.setClassEntity(classEntity);
                declaration = new FieldDeclaration();
            }
            declaration.setName(requireNonBlank(dto.name(), "Field name"));
            declaration.setDataType(requireNonBlank(dto.dataType(), "Field type"));
            declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "field scope"));
            declaration = fieldDeclarationRepository.save(declaration);
            field.setFieldDeclaration(declaration);
            field.setName(declaration.getName());
            field = fieldRepository.save(field);
            ctx.putField(field);
            kept.add(field.getId());
        }
        for (Field row : existing) {
            if (!kept.contains(row.getId())) {
                deleteField(ctx, row);
            }
        }
    }

    private void syncMethods(SaveContext ctx, ClassEntity classEntity, List<MethodStructureDTO> methodDtos) {
        List<Method> existing = List.copyOf(ctx.methodsByClassId.getOrDefault(classEntity.getId(), List.of()));
        Set<UUID> kept = new HashSet<>();
        List<MethodStructureDTO> payloads = methodDtos != null ? methodDtos : List.of();
        List<Method> savedMethods = new ArrayList<>();
        List<List<ParameterStructureDTO>> parameterPayloads = new ArrayList<>();

        for (MethodStructureDTO dto : payloads) {
            Method method;
            MethodDeclaration declaration;
            if (dto.id() != null) {
                method = ctx.methodsById.get(dto.id());
                if (method == null) {
                    method = new Method();
                    method.setId(dto.id());
                    method.setClassEntity(classEntity);
                    declaration = new MethodDeclaration();
                } else {
                    if (method.getClassEntity() != null
                            && !method.getClassEntity().getId().equals(classEntity.getId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Method does not belong to class");
                    }
                    if (method.getClassEntity() == null) {
                        method.setClassEntity(classEntity);
                    }
                    declaration = method.getMethodDeclaration();
                    if (declaration == null) {
                        declaration = new MethodDeclaration();
                    }
                }
            } else {
                method = new Method();
                method.setClassEntity(classEntity);
                declaration = new MethodDeclaration();
            }
            declaration.setName(requireNonBlank(dto.name(), "Method name"));
            declaration.setReturnType(requireNonBlank(dto.returnType(), "Return type"));
            declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "method scope"));
            declaration.setStatic(dto.isStatic());
            declaration.setAbstract(dto.isAbstract());
            declaration.setFinal(false);
            declaration = methodDeclarationRepository.save(declaration);
            method.setMethodDeclaration(declaration);
            method.setName(declaration.getName());
            method = methodRepository.save(method);
            ctx.putMethod(method);
            savedMethods.add(method);
            parameterPayloads.add(dto.parameters() != null ? dto.parameters() : List.of());
            kept.add(method.getId());
        }

        if (!savedMethods.isEmpty()) {
            List<UUID> methodIds = savedMethods.stream().map(Method::getId).toList();
            parameterRepository.deleteByMethod_IdIn(methodIds);
            List<Parameter> newParameters = new ArrayList<>();
            for (int i = 0; i < savedMethods.size(); i++) {
                newParameters.addAll(buildParameters(parameterPayloads.get(i), savedMethods.get(i), null));
            }
            if (!newParameters.isEmpty()) {
                parameterRepository.saveAll(newParameters);
            }
        }

        for (Method row : existing) {
            if (!kept.contains(row.getId())) {
                deleteMethod(ctx, row);
            }
        }
    }

    private void syncConstructors(SaveContext ctx, ClassEntity classEntity, List<ConstructorStructureDTO> constructorDtos) {
        List<Constructor> existing = List.copyOf(ctx.constructorsByClassId.getOrDefault(classEntity.getId(), List.of()));
        Set<UUID> kept = new HashSet<>();
        List<ConstructorStructureDTO> payloads = constructorDtos != null ? constructorDtos : List.of();
        List<Constructor> savedConstructors = new ArrayList<>();
        List<List<ParameterStructureDTO>> parameterPayloads = new ArrayList<>();

        for (ConstructorStructureDTO dto : payloads) {
            Constructor constructor;
            ConstructorDeclaration declaration;
            if (dto.id() != null) {
                constructor = ctx.constructorsById.get(dto.id());
                if (constructor == null) {
                    constructor = new Constructor();
                    constructor.setId(dto.id());
                    constructor.setClassEntity(classEntity);
                    declaration = new ConstructorDeclaration();
                } else {
                    if (constructor.getClassEntity() != null
                            && !constructor.getClassEntity().getId().equals(classEntity.getId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Constructor does not belong to class");
                    }
                    if (constructor.getClassEntity() == null) {
                        constructor.setClassEntity(classEntity);
                    }
                    declaration = constructor.getConstructorDeclaration();
                    if (declaration == null) {
                        declaration = new ConstructorDeclaration();
                    }
                }
            } else {
                constructor = new Constructor();
                constructor.setClassEntity(classEntity);
                declaration = new ConstructorDeclaration();
            }
            String constructorName = dto.name() != null && !dto.name().isBlank() ? dto.name().trim() : classEntity.getName();
            declaration.setName(constructorName);
            declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "constructor scope"));
            declaration.setDefault(dto.isDefault());
            declaration = constructorDeclarationRepository.save(declaration);
            constructor.setConstructorDeclaration(declaration);
            constructor.setName(constructorName);
            constructor = constructorRepository.save(constructor);
            ctx.putConstructor(constructor);
            savedConstructors.add(constructor);
            parameterPayloads.add(dto.parameters() != null ? dto.parameters() : List.of());
            kept.add(constructor.getId());
        }

        if (!savedConstructors.isEmpty()) {
            List<UUID> constructorIds = savedConstructors.stream().map(Constructor::getId).toList();
            parameterRepository.deleteByConstructorEntity_IdIn(constructorIds);
            List<Parameter> newParameters = new ArrayList<>();
            for (int i = 0; i < savedConstructors.size(); i++) {
                newParameters.addAll(buildParameters(parameterPayloads.get(i), null, savedConstructors.get(i)));
            }
            if (!newParameters.isEmpty()) {
                parameterRepository.saveAll(newParameters);
            }
        }

        for (Constructor row : existing) {
            if (!kept.contains(row.getId())) {
                deleteConstructor(ctx, row);
            }
        }
    }

    private List<Parameter> buildParameters(List<ParameterStructureDTO> parameterDtos, Method method, Constructor constructor) {
        if (parameterDtos == null || parameterDtos.isEmpty()) {
            return List.of();
        }
        List<Parameter> parameters = new ArrayList<>();
        int index = 0;
        for (ParameterStructureDTO dto : parameterDtos) {
            Parameter parameter = new Parameter();
            parameter.setMethod(method);
            parameter.setConstructorEntity(constructor);
            parameter.setName(requireNonBlank(dto.name(), "Parameter name"));
            parameter.setDataType(requireNonBlank(dto.dataType(), "Parameter type"));
            parameter.setOrderIndex(dto.orderIndex() >= 0 ? dto.orderIndex() : index);
            parameter.setFinal(dto.isFinal());
            parameters.add(parameter);
            index++;
        }
        return parameters;
    }

    private void deleteChallengeCascade(SaveContext ctx, Challenge challenge) {
        List<ClassEntity> classes = ctx.classesByChallengeId.getOrDefault(challenge.getId(), List.of());
        for (ClassEntity classEntity : List.copyOf(classes)) {
            deleteClassCascade(ctx, classEntity);
        }
        challengeRepository.delete(challenge);
        ctx.removeChallenge(challenge.getId());
    }

    private void deleteClassCascade(SaveContext ctx, ClassEntity classEntity) {
        UUID classId = classEntity.getId();
        UUID challengeId = classEntity.getChallenge().getId();
        deleteRelationsForClass(ctx, classId);
        for (Field field : List.copyOf(ctx.fieldsByClassId.getOrDefault(classId, List.of()))) {
            deleteField(ctx, field);
        }
        for (Method method : List.copyOf(ctx.methodsByClassId.getOrDefault(classId, List.of()))) {
            deleteMethod(ctx, method);
        }
        for (Constructor constructor : List.copyOf(ctx.constructorsByClassId.getOrDefault(classId, List.of()))) {
            deleteConstructor(ctx, constructor);
        }
        classEntityRepository.delete(classEntity);
        ctx.removeClass(classId, challengeId);
    }

    private void deleteField(SaveContext ctx, Field field) {
        UUID classId = field.getClassEntity().getId();
        UUID declarationId = field.getFieldDeclaration().getId();
        fieldRepository.delete(field);
        fieldDeclarationRepository.deleteById(declarationId);
        ctx.removeField(field.getId(), classId);
    }

    private void deleteMethod(SaveContext ctx, Method method) {
        UUID classId = method.getClassEntity().getId();
        parameterRepository.deleteByMethod_IdIn(List.of(method.getId()));
        UUID declarationId = method.getMethodDeclaration().getId();
        methodRepository.delete(method);
        methodDeclarationRepository.deleteById(declarationId);
        ctx.removeMethod(method.getId(), classId);
    }

    private void deleteConstructor(SaveContext ctx, Constructor constructor) {
        UUID classId = constructor.getClassEntity().getId();
        parameterRepository.deleteByConstructorEntity_IdIn(List.of(constructor.getId()));
        UUID declarationId = constructor.getConstructorDeclaration().getId();
        constructorRepository.delete(constructor);
        constructorDeclarationRepository.deleteById(declarationId);
        ctx.removeConstructor(constructor.getId(), classId);
    }

    private MasterData resolveMasterData(SaveContext ctx, Integer id, String label) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing " + label + " id");
        }
        MasterData masterData = ctx.masterDataById.get(id);
        if (masterData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + label + " id: " + id);
        }
        return masterData;
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
