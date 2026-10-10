package com.eiu.capstone.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabDeadlineRequest;
import com.eiu.capstone.backend.analytics.cache.LabStatisticsCache;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.UpdateLabStudentAccessRequest;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.grading.MmdComparisonService;
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
import com.eiu.capstone.backend.utility.TimingLog;

@Service
public class LabStructureService {

    /**
     * When true, skip per-batch flushes but still run {@link #flushStructurePhase()} at
     * cycle-breaking boundaries so Hibernate can JDBC-batch (outer_class / relation cycles).
     */
    private final ThreadLocal<Boolean> deferStructureFlush = ThreadLocal.withInitial(() -> Boolean.FALSE);

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
    private final TestcaseRubricService testcaseRubricService;
    private final LabStatisticsCache labStatisticsCache;
    private final LabDeadlineHelper labDeadlineHelper;
    private final EntityManager entityManager;
    private final LabStructureGraphLoader labStructureGraphLoader;
    private final boolean timingLog;

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
                               RubricCacheInvalidationSupport rubricCacheInvalidationSupport,
                               TestcaseRubricService testcaseRubricService,
                               LabStatisticsCache labStatisticsCache,
                               LabDeadlineHelper labDeadlineHelper,
                               EntityManager entityManager,
                               LabStructureGraphLoader labStructureGraphLoader,
                               @Value("${app.grading.timing-log:false}") boolean timingLog) {
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
        this.testcaseRubricService = testcaseRubricService;
        this.labStatisticsCache = labStatisticsCache;
        this.labDeadlineHelper = labDeadlineHelper;
        this.entityManager = entityManager;
        this.labStructureGraphLoader = labStructureGraphLoader;
        this.timingLog = timingLog;
    }

    @Transactional(readOnly = true)
    public LabStructureResponse loadForEditor(UUID labId) {
        long startedAt = System.currentTimeMillis();
        Lab lab = labRepository.findByIdWithTerm(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        LabStructureResponse response = buildLabStructureResponse(lab);
        TimingLog.block(timingLog, "Load lab structure (editor)",
                "total", System.currentTimeMillis() - startedAt);
        return response;
    }

    private LabStructureResponse buildLabStructureResponse(Lab lab) {
        UUID labId = lab.getId();
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        if (challenges.isEmpty()) {
            return new LabStructureResponse(
                    lab.getId(), lab.getName(), lab.getTerm().getId(), lab.getDeadlineDate(),
                    lab.isStudentVisible(), lab.getReleaseDate(),
                    List.of());
        }

        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        List<UUID> classIds = classes.stream().map(ClassEntity::getId).toList();
        LabStructureGraphLoader.MemberWave members = labStructureGraphLoader != null
                ? labStructureGraphLoader.loadMemberWave(classIds)
                : loadMemberWaveSerial(classes);
        List<Field> fields = members.fields();
        List<Method> methods = members.methods();
        List<Constructor> constructors = members.constructors();
        List<ClassRelation> allRelations = members.relations();

        List<UUID> methodIds = methods.stream().map(Method::getId).toList();
        List<UUID> constructorIds = constructors.stream().map(Constructor::getId).toList();
        LabStructureGraphLoader.ParameterWave params = labStructureGraphLoader != null
                ? labStructureGraphLoader.loadParameterWave(methodIds, constructorIds)
                : loadParameterWaveSerial(methods, constructors);
        List<Parameter> methodParams = params.methodParams();
        List<Parameter> constructorParams = params.constructorParams();

        Map<UUID, List<ClassEntity>> classesByChallenge = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        Map<UUID, List<Field>> fieldsByClass = fields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = methods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = constructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<Parameter>> paramsByMethod = methodParams.stream()
                .filter(p -> p.getMethod() != null)
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        Map<UUID, List<Parameter>> paramsByConstructor = constructorParams.stream()
                .filter(p -> p.getConstructorEntity() != null)
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

        return new LabStructureResponse(
                lab.getId(), lab.getName(), lab.getTerm().getId(), lab.getDeadlineDate(),
                lab.isStudentVisible(), lab.getReleaseDate(),
                challengeDtos);
    }

    private LabStructureGraphLoader.MemberWave loadMemberWaveSerial(List<ClassEntity> classes) {
        if (classes.isEmpty()) {
            return new LabStructureGraphLoader.MemberWave(List.of(), List.of(), List.of(), List.of());
        }
        return new LabStructureGraphLoader.MemberWave(
                fieldRepository.findByClassEntityInWithDeclaration(classes),
                methodRepository.findByClassEntityInWithDeclaration(classes),
                constructorRepository.findByClassEntityInWithDeclaration(classes),
                classRelationRepository.findByClassEntityInWithEndpoints(classes));
    }

    private LabStructureGraphLoader.ParameterWave loadParameterWaveSerial(List<Method> methods,
                                                                           List<Constructor> constructors) {
        List<Parameter> methodParams = methods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(methods);
        List<Parameter> constructorParams = constructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(constructors);
        return new LabStructureGraphLoader.ParameterWave(methodParams, constructorParams);
    }

    /**
     * Clone insert path: same as editor save but skips loading an empty tree
     * and defers flushes to one commit flush (Neon RTT).
     */
    @Transactional
    public LabStructureResponse saveLabStructureInsertOnly(UUID labId, LabStructureResponse payload) {
        deferStructureFlush.set(Boolean.TRUE);
        try {
            LabStructureResponse saved = syncLabStructure(labId, payload, true, List.of());
            entityManager.flush();
            return saved;
        } finally {
            deferStructureFlush.remove();
        }
    }

    @Transactional
    public LabStructureResponse saveLabStructure(UUID labId, LabStructureResponse payload) {
        return saveLabStructure(labId, payload, List.of());
    }

    /**
     * Editor save: defer flushes to one commit flush; optional {@code replacedChallengeIds}
     * wipe OT once and rebuild those challenges insert-style (solution-import replaces).
     */
    @Transactional
    public LabStructureResponse saveLabStructure(UUID labId,
                                                 LabStructureResponse payload,
                                                 List<UUID> replacedChallengeIds) {
        deferStructureFlush.set(Boolean.TRUE);
        try {
            LabStructureResponse saved = syncLabStructure(
                    labId, payload, false, replacedChallengeIds != null ? replacedChallengeIds : List.of());
            entityManager.flush();
            return saved;
        } finally {
            deferStructureFlush.remove();
        }
    }

    private LabStructureResponse syncLabStructure(UUID labId,
                                                  LabStructureResponse payload,
                                                  boolean insertOnly,
                                                  List<UUID> replacedChallengeIds) {
        long startedAt = System.currentTimeMillis();
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (!Objects.equals(labId, payload.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab id mismatch");
        }

        List<ChallengeStructureDTO> challengePayloads = payload.challenges() != null ? payload.challenges() : List.of();
        long loadStartedAt = System.currentTimeMillis();
        SaveContext ctx = insertOnly ? emptySaveContextForInsert() : loadSaveContext(labId);
        long loadMs = System.currentTimeMillis() - loadStartedAt;
        Set<UUID> keptChallengeIds = new HashSet<>();

        if (!insertOnly) {
            List<UUID> replacedOnLab = replacedChallengeIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .filter(ctx.challengesById::containsKey)
                    .toList();
            if (!replacedOnLab.isEmpty()) {
                // Keep Challenge rows (stable ids / submission FKs). Wipe OT + class trees,
                // flush deletes, then upsert fields and insert-style sync children.
                testcaseRubricService.deleteAllForChallenges(replacedOnLab);
                for (UUID challengeId : replacedOnLab) {
                    List<ClassEntity> classes = List.copyOf(
                            ctx.classesByChallengeId.getOrDefault(challengeId, List.of()));
                    for (ClassEntity classEntity : classes) {
                        deleteClassCascade(ctx, classEntity, true);
                    }
                }
                entityManager.flush();
            }

            Set<UUID> payloadChallengeIds = challengePayloads.stream()
                    .map(ChallengeStructureDTO::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Challenge existing : List.copyOf(ctx.challengesById.values())) {
                if (!payloadChallengeIds.contains(existing.getId())) {
                    deleteChallengeCascade(ctx, existing);
                }
            }
            flushStructure();
        }

        Set<Integer> usedChallengeNumbers = ctx.challengesById.values().stream()
                .map(Challenge::getChallengeNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        Set<UUID> replacedSet = replacedChallengeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long syncStartedAt = System.currentTimeMillis();
        boolean phased = Boolean.TRUE.equals(deferStructureFlush.get());
        List<PendingOuter> pendingOuters = new ArrayList<>();
        List<PendingRelations> pendingRelations = new ArrayList<>();
        for (ChallengeStructureDTO challengeDto : challengePayloads) {
            Challenge challenge = upsertChallenge(ctx, lab, challengeDto, usedChallengeNumbers, keptChallengeIds);
            if (!insertOnly
                    && challengeDto.id() != null
                    && !replacedSet.contains(challengeDto.id())
                    && challengeTreeUnchanged(ctx, challengeDto)) {
                continue;
            }
            syncClasses(ctx, challenge, challengeDto.classes(), phased ? pendingOuters : null);
            if (phased) {
                pendingRelations.add(new PendingRelations(challenge, challengeDto.relations()));
            } else {
                syncRelations(ctx, challenge, challengeDto.relations());
            }
        }
        if (phased) {
            // Phase 1: challenges + class shells (outer null) + members + params — batchable.
            flushStructurePhase();
            for (PendingOuter pending : pendingOuters) {
                applyOuterClass(ctx, Map.of(), pending.challenge(), pending.classEntity(), pending.dto());
            }
            // Phase 2: outer_class UPDATEs only (breaks self-FK cycle with inserts).
            if (!pendingOuters.isEmpty()) {
                flushStructurePhase();
            }
            for (PendingRelations pending : pendingRelations) {
                syncRelations(ctx, pending.challenge(), pending.relations());
            }
            // Phase 3 relations flush at save* final entityManager.flush().
        }
        long syncMs = System.currentTimeMillis() - syncStartedAt;

        if (payload.name() != null && !payload.name().isBlank()) {
            lab.setName(com.eiu.capstone.backend.desktop.pack.LabNameRules.requireValid(payload.name()));
        }
        lab.setDeadlineDate(payload.deadlineDate());
        labRepository.save(lab);
        labStatisticsCache.invalidate(labId);

        rubricCacheInvalidationSupport.invalidateLab(labId);
        String labName = lab.getName();
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
                            dto.relations(),
                            saved.isHasMmd(),
                            saved.getWeight(),
                            saved.getClassWeight(),
                            saved.getMmdWeight(),
                            saved.getTestcaseWeight());
                })
                .toList();
        TimingLog.block(timingLog, insertOnly ? "Save lab structure (insert-only)" : "Save lab structure",
                "load", loadMs,
                "sync", syncMs,
                "total", System.currentTimeMillis() - startedAt);
        return new LabStructureResponse(
                labId, labName, lab.getTerm().getId(), lab.getDeadlineDate(),
                lab.isStudentVisible(), lab.getReleaseDate(),
                savedChallenges);
    }

    @Transactional
    public LabStructureResponse updateLabDeadline(UUID labId, UpdateLabDeadlineRequest request) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        lab.setDeadlineDate(request.deadlineDate());
        labRepository.save(lab);
        labStatisticsCache.invalidate(labId);
        return buildLabStructureResponse(lab);
    }

    @Transactional
    public LabStructureResponse updateLabStudentAccess(UUID labId, UpdateLabStudentAccessRequest request) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (request.studentVisible() != null) {
            lab.setStudentVisible(request.studentVisible());
        }
        labRepository.save(lab);
        return buildLabStructureResponse(lab);
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

        List<Parameter> methodParams = methods.isEmpty() ? List.of() : parameterRepository.findByMethodIn(methods);
        List<Parameter> constructorParams = constructors.isEmpty() ? List.of()
                : parameterRepository.findByConstructorEntityIn(constructors);
        Map<UUID, List<Parameter>> paramsByMethodId = methodParams.stream()
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        Map<UUID, List<Parameter>> paramsByConstructorId = constructorParams.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));

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
                paramsByMethodId,
                paramsByConstructorId,
                masterDataById);
    }

    /** Empty tree + master data only — clone insert path skips empty Neon lookups. */
    private SaveContext emptySaveContextForInsert() {
        Map<Integer, MasterData> masterDataById = masterDataRepository.findAll().stream()
                .collect(Collectors.toMap(MasterData::getId, Function.identity()));
        return new SaveContext(
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
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
        final Map<UUID, List<Parameter>> paramsByMethodId;
        final Map<UUID, List<Parameter>> paramsByConstructorId;
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
                    Map<UUID, List<Parameter>> paramsByMethodId,
                    Map<UUID, List<Parameter>> paramsByConstructorId,
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
            this.paramsByMethodId = new HashMap<>(paramsByMethodId);
            this.paramsByConstructorId = new HashMap<>(paramsByConstructorId);
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
        String labName = com.eiu.capstone.backend.desktop.pack.LabNameRules.requireValid(request.name());
        if (request.termId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "termId is required");
        }
        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid termId"));
        Lab lab = new Lab();
        // Lab.id has no @GeneratedValue so desktop pack import can keep a preset UUID.
        lab.setId(UUID.randomUUID());
        lab.setName(labName);
        lab.setTerm(term);
        LocalDate deadline = request.deadlineDate() != null ? request.deadlineDate() : term.getEndDate();
        lab.setDeadlineDate(deadline);
        lab = labRepository.save(lab);
        return new LabStructureResponse(
                lab.getId(), lab.getName(), term.getId(), lab.getDeadlineDate(),
                lab.isStudentVisible(), lab.getReleaseDate(),
                List.of());
    }

    @Transactional
    public void deleteLabCascade(UUID labId) {
        deleteLabsCascadeBulk(List.of(labId));
    }

    /**
     * Deletes labs and all dependent rows with a fixed sequence of set-based SQL statements
     * (Neon RTT-friendly). Prefer this for term delete over per-entity cascades.
     */
    @Transactional
    public void deleteLabsCascadeBulk(Collection<UUID> labIds) {
        long startedAt = System.currentTimeMillis();
        List<UUID> ids = labIds == null
                ? List.of()
                : labIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        deleteLabsByIdsNative(ids);
        for (UUID id : ids) {
            rubricCacheInvalidationSupport.invalidateLab(id);
            labStatisticsCache.invalidate(id);
        }
        TimingLog.block(timingLog, "Delete labs cascade (bulk)",
                "labs", ids.size(),
                "total", System.currentTimeMillis() - startedAt);
    }

    /**
     * Set-based wipe for one or more labs. Order respects FKs; no per-row entity deletes.
     */
    private void deleteLabsByIdsNative(List<UUID> labIds) {
        // --- runtime / grading residuals (by lab_id) ---
        execDelete("DELETE FROM submission_plagiarism_match WHERE lab_id IN (:labIds)", labIds);
        execDelete("DELETE FROM submission_plagiarism_fingerprint WHERE lab_id IN (:labIds)", labIds);
        execDelete("""
                DELETE FROM submission_testcase_assertion_result
                WHERE submission_testcase_result_id IN (
                    SELECT str.id
                    FROM submission_testcase_result str
                    JOIN lab_submission s ON s.id = str.submission_id
                    WHERE s.lab_id IN (:labIds)
                )
                """, labIds);
        for (String table : List.of(
                "submission_testcase_result",
                "submission_field_result",
                "submission_method_result",
                "submission_constructor_result",
                "submission_challenge_result",
                "submission_relation_result")) {
            execDelete(
                    "DELETE FROM " + table
                            + " WHERE submission_id IN (SELECT id FROM lab_submission WHERE lab_id IN (:labIds))",
                    labIds);
        }
        execDelete("DELETE FROM student_lab_progress WHERE lab_id IN (:labIds)", labIds);
        execDelete("DELETE FROM lab_deadline_email_sent WHERE lab_id IN (:labIds)", labIds);
        execDelete("DELETE FROM lab_submission WHERE lab_id IN (:labIds)", labIds);

        // --- operational testcases ---
        execDelete("""
                DELETE FROM testcase_assertion
                WHERE testcase_id IN (
                    SELECT tc.id FROM testcase tc
                    JOIN challenge ch ON ch.id = tc.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        execDelete("""
                DELETE FROM testcase_invocation
                WHERE testcase_id IN (
                    SELECT tc.id FROM testcase tc
                    JOIN challenge ch ON ch.id = tc.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        execDelete("""
                DELETE FROM testcase
                WHERE challenge_id IN (SELECT id FROM challenge WHERE lab_id IN (:labIds))
                """, labIds);

        // --- rubric members + declarations ---
        execDelete("""
                DELETE FROM parameter
                WHERE method_id IN (
                    SELECT m.id FROM method m
                    JOIN class_entity ce ON ce.id = m.class_id
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                OR constructor_id IN (
                    SELECT c.id FROM constructor c
                    JOIN class_entity ce ON ce.id = c.class_id
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        deleteFieldsAndDeclarationsForLabs(labIds);
        deleteMethodsAndDeclarationsForLabs(labIds);
        deleteConstructorsAndDeclarationsForLabs(labIds);
        execDelete("""
                DELETE FROM class_relation
                WHERE class_id IN (
                    SELECT ce.id FROM class_entity ce
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                OR target_class_id IN (
                    SELECT ce.id FROM class_entity ce
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        // Break nested-class self-FK before deleting shells.
        execDelete("""
                UPDATE class_entity SET outer_class_id = NULL
                WHERE challenge_id IN (SELECT id FROM challenge WHERE lab_id IN (:labIds))
                """, labIds);
        execDelete("""
                DELETE FROM class_entity
                WHERE challenge_id IN (SELECT id FROM challenge WHERE lab_id IN (:labIds))
                """, labIds);
        execDelete("DELETE FROM challenge WHERE lab_id IN (:labIds)", labIds);
        execDelete("DELETE FROM lab WHERE id IN (:labIds)", labIds);
    }

    private void execDelete(String sql, List<UUID> labIds) {
        entityManager.createNativeQuery(sql)
                .setParameter("labIds", labIds)
                .executeUpdate();
    }

    /**
     * Removes rubric field rows for the labs, then their declarations. Uses select-then-delete
     * so the same path works on PostgreSQL and H2 (desktop); PG {@code WITH ... DELETE RETURNING}
     * CTEs are not supported on H2.
     */
    private void deleteFieldsAndDeclarationsForLabs(List<UUID> labIds) {
        @SuppressWarnings("unchecked")
        List<UUID> declIds = entityManager.createNativeQuery("""
                SELECT DISTINCT f.field_declaration_id
                FROM field f
                JOIN class_entity ce ON ce.id = f.class_id
                JOIN challenge ch ON ch.id = ce.challenge_id
                WHERE ch.lab_id IN (:labIds) AND f.field_declaration_id IS NOT NULL
                """)
                .setParameter("labIds", labIds)
                .getResultList();
        execDelete("""
                DELETE FROM field WHERE id IN (
                    SELECT f.id FROM field f
                    JOIN class_entity ce ON ce.id = f.class_id
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        deleteDeclarationsByIds("field_declaration", declIds);
    }

    private void deleteMethodsAndDeclarationsForLabs(List<UUID> labIds) {
        @SuppressWarnings("unchecked")
        List<UUID> declIds = entityManager.createNativeQuery("""
                SELECT DISTINCT m.method_declaration_id
                FROM method m
                JOIN class_entity ce ON ce.id = m.class_id
                JOIN challenge ch ON ch.id = ce.challenge_id
                WHERE ch.lab_id IN (:labIds) AND m.method_declaration_id IS NOT NULL
                """)
                .setParameter("labIds", labIds)
                .getResultList();
        execDelete("""
                DELETE FROM method WHERE id IN (
                    SELECT m.id FROM method m
                    JOIN class_entity ce ON ce.id = m.class_id
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        deleteDeclarationsByIds("method_declaration", declIds);
    }

    private void deleteConstructorsAndDeclarationsForLabs(List<UUID> labIds) {
        @SuppressWarnings("unchecked")
        List<UUID> declIds = entityManager.createNativeQuery("""
                SELECT DISTINCT c.constructor_declaration_id
                FROM constructor c
                JOIN class_entity ce ON ce.id = c.class_id
                JOIN challenge ch ON ch.id = ce.challenge_id
                WHERE ch.lab_id IN (:labIds) AND c.constructor_declaration_id IS NOT NULL
                """)
                .setParameter("labIds", labIds)
                .getResultList();
        execDelete("""
                DELETE FROM constructor WHERE id IN (
                    SELECT c.id FROM constructor c
                    JOIN class_entity ce ON ce.id = c.class_id
                    JOIN challenge ch ON ch.id = ce.challenge_id
                    WHERE ch.lab_id IN (:labIds)
                )
                """, labIds);
        deleteDeclarationsByIds("constructor_declaration", declIds);
    }

    private void deleteDeclarationsByIds(String table, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        entityManager.createNativeQuery("DELETE FROM " + table + " WHERE id IN (:ids)")
                .setParameter("ids", ids)
                .executeUpdate();
    }

    private void flushStructure() {
        if (Boolean.TRUE.equals(deferStructureFlush.get())) {
            return;
        }
        entityManager.flush();
    }

    /** Always flush — used at cycle-breaking phase boundaries under deferred sync. */
    private void flushStructurePhase() {
        entityManager.flush();
    }

    private record PendingOuter(Challenge challenge, ClassEntity classEntity, ClassStructureDTO dto) {}

    private record PendingRelations(Challenge challenge, List<RelationStructureDTO> relations) {}

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
        challenge.setHasMmd(dto.hasMmd());
        challenge.setWeight(normalizeWeight(dto.weight()));
        challenge.setClassWeight(normalizeWeight(dto.classWeight()));
        challenge.setMmdWeight(normalizeWeight(dto.mmdWeight()));
        challenge.setTestcaseWeight(normalizeWeight(dto.testcaseWeight()));
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
        if (isNew) {
            if (challenge.getId() == null) {
                challenge.setId(UUID.randomUUID());
            }
            // Client/clone UUIDs must not go through Spring Data save→merge (can replace the id).
            entityManager.persist(challenge);
        } else {
            challenge = challengeRepository.save(challenge);
        }
        flushStructure();
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

    private void syncClasses(SaveContext ctx,
                             Challenge challenge,
                             List<ClassStructureDTO> classDtos,
                             List<PendingOuter> pendingOuters) {
        List<ClassEntity> existingClasses = List.copyOf(ctx.classesByChallengeId.getOrDefault(challenge.getId(), List.of()));
        Set<UUID> keptClassIds = new HashSet<>();
        List<ClassStructureDTO> payloads = classDtos != null ? classDtos : List.of();

        // Prepare shells first (no outer yet) so same-save nested classes can resolve outer from the batch.
        List<ClassEntity> prepared = new ArrayList<>(payloads.size());
        List<Boolean> isNewFlags = new ArrayList<>(payloads.size());
        Map<UUID, ClassEntity> batchById = new LinkedHashMap<>();
        for (ClassStructureDTO classDto : payloads) {
            boolean isNew = classDto.id() == null || !ctx.classesById.containsKey(classDto.id());
            ClassEntity shell = prepareClassShell(ctx, challenge, classDto);
            if (shell.getId() == null) {
                shell.setId(UUID.randomUUID());
            }
            prepared.add(shell);
            isNewFlags.add(isNew);
            batchById.put(shell.getId(), shell);
        }
        if (pendingOuters != null) {
            // Defer outer_class so class INSERTs are not in a circular Hibernate batch with self-FKs.
            for (int i = 0; i < payloads.size(); i++) {
                if (payloads.get(i).outerClassId() != null) {
                    pendingOuters.add(new PendingOuter(challenge, prepared.get(i), payloads.get(i)));
                }
            }
        } else {
            for (int i = 0; i < payloads.size(); i++) {
                applyOuterClass(ctx, batchById, challenge, prepared.get(i), payloads.get(i));
            }
        }

        for (int i = 0; i < prepared.size(); i++) {
            ClassEntity classEntity = prepared.get(i);
            if (isNewFlags.get(i)) {
                entityManager.persist(classEntity);
            } else {
                classEntityRepository.save(classEntity);
            }
        }
        flushStructure();
        for (ClassEntity classEntity : prepared) {
            ctx.putClass(classEntity);
            keptClassIds.add(classEntity.getId());
        }
        if (!prepared.isEmpty()) {
            syncFieldsBatch(ctx, prepared, payloads);
            syncMethodsBatch(ctx, prepared, payloads);
            syncConstructorsBatch(ctx, prepared, payloads);
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
        List<ClassRelation> relationsToSave = new ArrayList<>();

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
            boolean isNew;
            if (dto.id() != null) {
                relation = ctx.relationsById.get(dto.id());
                if (relation == null) {
                    relation = new ClassRelation();
                    relation.setId(dto.id());
                    isNew = true;
                } else {
                    isNew = false;
                }
            } else {
                relation = new ClassRelation();
                relation.setId(UUID.randomUUID());
                isNew = true;
            }
            relation.setClassEntity(source);
            relation.setTargetClassEntity(target);
            relation.setRelationType(resolveMasterData(ctx, dto.relationTypeId(), "relation type"));
            if (isNew) {
                entityManager.persist(relation);
            } else {
                classRelationRepository.save(relation);
            }
            relationsToSave.add(relation);
        }

        rejectExtraHeritagePairs(relationsToSave);

        if (!relationsToSave.isEmpty()) {
            flushStructure();
            for (ClassRelation relation : relationsToSave) {
                ctx.putRelation(relation);
                kept.add(relation.getId());
            }
        }

        for (ClassRelation row : existing) {
            if (!kept.contains(row.getId())) {
                classRelationRepository.delete(row);
                ctx.removeRelation(row.getId(), challenge.getId());
            }
        }
    }

    private void rejectExtraHeritagePairs(List<ClassRelation> relationsToSave) {
        // Java: one extends (inheritance), many implements (realization). Class-shell grading
        // still picks at most one heritage row; MMD grades each relation independently.
        Map<UUID, Integer> inheritanceCountBySource = new HashMap<>();
        for (ClassRelation relation : relationsToSave) {
            if (!isInheritanceRelation(relation.getRelationType())) {
                continue;
            }
            UUID sourceId = relation.getClassEntity().getId();
            int count = inheritanceCountBySource.merge(sourceId, 1, Integer::sum);
            if (count > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A class can have at most one inheritance (extends) relationship");
            }
        }
    }

    private static boolean isInheritanceRelation(MasterData relationType) {
        if (relationType == null || relationType.getName() == null) {
            return false;
        }
        return "inheritance".equals(MmdComparisonService.normalizeRelationTypeName(relationType.getName()));
    }

    private void deleteRelationsForClass(SaveContext ctx, UUID classId) {
        // Prefer SaveContext (already loaded) — avoids 2 Neon SELECTs per deleted class.
        List<ClassRelation> touching = ctx.relationsById.values().stream()
                .filter(relation -> {
                    UUID sourceId = relation.getClassEntity() != null ? relation.getClassEntity().getId() : null;
                    UUID targetId = relation.getTargetClassEntity() != null
                            ? relation.getTargetClassEntity().getId() : null;
                    return classId.equals(sourceId) || classId.equals(targetId);
                })
                .toList();
        for (ClassRelation relation : touching) {
            UUID challengeId = relation.getClassEntity().getChallenge().getId();
            classRelationRepository.delete(relation);
            ctx.removeRelation(relation.getId(), challengeId);
        }
    }

    private ClassEntity prepareClassShell(SaveContext ctx, Challenge challenge, ClassStructureDTO dto) {
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
        classEntity.setWeight(normalizeWeight(dto.weight()));
        classEntity.setOuterClass(null);
        classEntity.setStatic(false);
        return classEntity;
    }

    private void applyOuterClass(SaveContext ctx,
                                 Map<UUID, ClassEntity> batchById,
                                 Challenge challenge,
                                 ClassEntity classEntity,
                                 ClassStructureDTO dto) {
        if (dto.outerClassId() == null) {
            classEntity.setOuterClass(null);
            classEntity.setStatic(false);
            return;
        }
        if (dto.id() != null && dto.id().equals(dto.outerClassId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class cannot be its own outer class");
        }
        ClassEntity outer = ctx.classesById.get(dto.outerClassId());
        if (outer == null) {
            outer = batchById.get(dto.outerClassId());
        }
        if (outer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid outer class id: " + dto.outerClassId());
        }
        if (outer.getChallenge() == null || !outer.getChallenge().getId().equals(challenge.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outer class must belong to the same problem");
        }
        if (outer.getOuterClass() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outer class must be a top-level class");
        }
        classEntity.setOuterClass(outer);
        classEntity.setStatic(dto.isStatic());
    }

    private void syncFieldsBatch(SaveContext ctx, List<ClassEntity> classes, List<ClassStructureDTO> classDtos) {
        List<FieldDeclaration> declarationsToSave = new ArrayList<>();
        List<Field> fieldsToSave = new ArrayList<>();
        List<Boolean> isNewFlags = new ArrayList<>();
        Map<UUID, Set<UUID>> keptByClassId = new HashMap<>();

        for (int i = 0; i < classes.size(); i++) {
            ClassEntity classEntity = classes.get(i);
            List<FieldStructureDTO> fieldDtos = classDtos.get(i).fields();
            if (fieldDtos == null) {
                continue;
            }
            for (FieldStructureDTO dto : fieldDtos) {
                Field field;
                FieldDeclaration declaration;
                boolean isNew;
                if (dto.id() != null) {
                    field = ctx.fieldsById.get(dto.id());
                    if (field == null) {
                        field = new Field();
                        field.setId(dto.id());
                        field.setClassEntity(classEntity);
                        declaration = new FieldDeclaration();
                        isNew = true;
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
                        isNew = false;
                    }
                } else {
                    field = new Field();
                    field.setId(UUID.randomUUID());
                    field.setClassEntity(classEntity);
                    declaration = new FieldDeclaration();
                    isNew = true;
                }
                declaration.setName(requireNonBlank(dto.name(), "Field name"));
                declaration.setDataType(requireNonBlank(dto.dataType(), "Field type"));
                declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "field scope"));
                declarationsToSave.add(declaration);
                fieldsToSave.add(field);
                isNewFlags.add(isNew);
            }
        }

        if (!declarationsToSave.isEmpty()) {
            for (int i = 0; i < fieldsToSave.size(); i++) {
                FieldDeclaration declaration = declarationsToSave.get(i);
                Field field = fieldsToSave.get(i);
                if (isNewFlags.get(i)) {
                    entityManager.persist(declaration);
                    field.setFieldDeclaration(declaration);
                    field.setName(declaration.getName());
                    entityManager.persist(field);
                } else {
                    FieldDeclaration savedDecl = fieldDeclarationRepository.save(declaration);
                    field.setFieldDeclaration(savedDecl);
                    field.setName(savedDecl.getName());
                    fieldRepository.save(field);
                }
            }
            flushStructure();
            for (Field field : fieldsToSave) {
                ctx.putField(field);
                keptByClassId.computeIfAbsent(field.getClassEntity().getId(), ignored -> new HashSet<>())
                        .add(field.getId());
            }
        }

        for (ClassEntity classEntity : classes) {
            Set<UUID> kept = keptByClassId.getOrDefault(classEntity.getId(), Set.of());
            List<Field> existing = List.copyOf(ctx.fieldsByClassId.getOrDefault(classEntity.getId(), List.of()));
            for (Field row : existing) {
                if (!kept.contains(row.getId())) {
                    deleteField(ctx, row);
                }
            }
        }
    }

    private void syncMethodsBatch(SaveContext ctx, List<ClassEntity> classes, List<ClassStructureDTO> classDtos) {
        List<MethodDeclaration> declarationsToSave = new ArrayList<>();
        List<Method> methodsToSave = new ArrayList<>();
        List<Boolean> isNewFlags = new ArrayList<>();
        List<List<ParameterStructureDTO>> parameterPayloads = new ArrayList<>();
        Map<UUID, Set<UUID>> keptByClassId = new HashMap<>();

        for (int i = 0; i < classes.size(); i++) {
            ClassEntity classEntity = classes.get(i);
            List<MethodStructureDTO> methodDtos = classDtos.get(i).methods();
            if (methodDtos == null) {
                continue;
            }
            for (MethodStructureDTO dto : methodDtos) {
                Method method;
                MethodDeclaration declaration;
                boolean isNew;
                if (dto.id() != null) {
                    method = ctx.methodsById.get(dto.id());
                    if (method == null) {
                        method = new Method();
                        method.setId(dto.id());
                        method.setClassEntity(classEntity);
                        declaration = new MethodDeclaration();
                        isNew = true;
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
                        isNew = false;
                    }
                } else {
                    method = new Method();
                    method.setId(UUID.randomUUID());
                    method.setClassEntity(classEntity);
                    declaration = new MethodDeclaration();
                    isNew = true;
                }
                declaration.setName(requireNonBlank(dto.name(), "Method name"));
                declaration.setReturnType(requireNonBlank(dto.returnType(), "Return type"));
                declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "method scope"));
                declaration.setStatic(dto.isStatic());
                declaration.setAbstract(dto.isAbstract());
                declaration.setFinal(false);
                declarationsToSave.add(declaration);
                methodsToSave.add(method);
                isNewFlags.add(isNew);
                parameterPayloads.add(dto.parameters() != null ? dto.parameters() : List.of());
            }
        }

        if (!declarationsToSave.isEmpty()) {
            for (int i = 0; i < methodsToSave.size(); i++) {
                MethodDeclaration declaration = declarationsToSave.get(i);
                Method method = methodsToSave.get(i);
                if (isNewFlags.get(i)) {
                    entityManager.persist(declaration);
                    method.setMethodDeclaration(declaration);
                    method.setName(declaration.getName());
                    entityManager.persist(method);
                } else {
                    MethodDeclaration savedDecl = methodDeclarationRepository.save(declaration);
                    method.setMethodDeclaration(savedDecl);
                    method.setName(savedDecl.getName());
                    methodRepository.save(method);
                }
            }
            flushStructure();
            List<UUID> methodIds = methodsToSave.stream().map(Method::getId).toList();
            parameterRepository.deleteByMethod_IdIn(methodIds);
            List<Parameter> newParameters = new ArrayList<>();
            for (int i = 0; i < methodsToSave.size(); i++) {
                newParameters.addAll(buildParameters(parameterPayloads.get(i), methodsToSave.get(i), null));
            }
            for (Parameter parameter : newParameters) {
                entityManager.persist(parameter);
            }
            flushStructure();
            for (Method method : methodsToSave) {
                ctx.putMethod(method);
                keptByClassId.computeIfAbsent(method.getClassEntity().getId(), ignored -> new HashSet<>())
                        .add(method.getId());
            }
        }

        for (ClassEntity classEntity : classes) {
            Set<UUID> kept = keptByClassId.getOrDefault(classEntity.getId(), Set.of());
            List<Method> existing = List.copyOf(ctx.methodsByClassId.getOrDefault(classEntity.getId(), List.of()));
            for (Method row : existing) {
                if (!kept.contains(row.getId())) {
                    deleteMethod(ctx, row);
                }
            }
        }
    }

    private void syncConstructorsBatch(SaveContext ctx, List<ClassEntity> classes, List<ClassStructureDTO> classDtos) {
        List<ConstructorDeclaration> declarationsToSave = new ArrayList<>();
        List<Constructor> constructorsToSave = new ArrayList<>();
        List<Boolean> isNewFlags = new ArrayList<>();
        List<List<ParameterStructureDTO>> parameterPayloads = new ArrayList<>();
        Map<UUID, Set<UUID>> keptByClassId = new HashMap<>();

        for (int i = 0; i < classes.size(); i++) {
            ClassEntity classEntity = classes.get(i);
            List<ConstructorStructureDTO> constructorDtos = classDtos.get(i).constructors();
            if (constructorDtos == null) {
                continue;
            }
            for (ConstructorStructureDTO dto : constructorDtos) {
                Constructor constructor;
                ConstructorDeclaration declaration;
                boolean isNew;
                if (dto.id() != null) {
                    constructor = ctx.constructorsById.get(dto.id());
                    if (constructor == null) {
                        constructor = new Constructor();
                        constructor.setId(dto.id());
                        constructor.setClassEntity(classEntity);
                        declaration = new ConstructorDeclaration();
                        isNew = true;
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
                        isNew = false;
                    }
                } else {
                    constructor = new Constructor();
                    constructor.setId(UUID.randomUUID());
                    constructor.setClassEntity(classEntity);
                    declaration = new ConstructorDeclaration();
                    isNew = true;
                }
                String constructorName = dto.name() != null && !dto.name().isBlank()
                        ? dto.name().trim()
                        : classEntity.getName();
                declaration.setName(constructorName);
                declaration.setScope(resolveMasterData(ctx, dto.scopeId(), "constructor scope"));
                declaration.setDefault(dto.isDefault());
                declarationsToSave.add(declaration);
                constructorsToSave.add(constructor);
                isNewFlags.add(isNew);
                parameterPayloads.add(dto.parameters() != null ? dto.parameters() : List.of());
            }
        }

        if (!declarationsToSave.isEmpty()) {
            for (int i = 0; i < constructorsToSave.size(); i++) {
                ConstructorDeclaration declaration = declarationsToSave.get(i);
                Constructor constructor = constructorsToSave.get(i);
                if (isNewFlags.get(i)) {
                    entityManager.persist(declaration);
                    constructor.setConstructorDeclaration(declaration);
                    constructor.setName(declaration.getName());
                    entityManager.persist(constructor);
                } else {
                    ConstructorDeclaration savedDecl = constructorDeclarationRepository.save(declaration);
                    constructor.setConstructorDeclaration(savedDecl);
                    constructor.setName(savedDecl.getName());
                    constructorRepository.save(constructor);
                }
            }
            flushStructure();
            List<UUID> constructorIds = constructorsToSave.stream().map(Constructor::getId).toList();
            parameterRepository.deleteByConstructorEntity_IdIn(constructorIds);
            List<Parameter> newParameters = new ArrayList<>();
            for (int i = 0; i < constructorsToSave.size(); i++) {
                newParameters.addAll(buildParameters(parameterPayloads.get(i), null, constructorsToSave.get(i)));
            }
            for (Parameter parameter : newParameters) {
                entityManager.persist(parameter);
            }
            flushStructure();
            for (Constructor constructor : constructorsToSave) {
                ctx.putConstructor(constructor);
                keptByClassId.computeIfAbsent(constructor.getClassEntity().getId(), ignored -> new HashSet<>())
                        .add(constructor.getId());
            }
        }

        for (ClassEntity classEntity : classes) {
            Set<UUID> kept = keptByClassId.getOrDefault(classEntity.getId(), Set.of());
            List<Constructor> existing = List.copyOf(ctx.constructorsByClassId.getOrDefault(classEntity.getId(), List.of()));
            for (Constructor row : existing) {
                if (!kept.contains(row.getId())) {
                    deleteConstructor(ctx, row);
                }
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
        deleteChallengeCascade(ctx, challenge, false);
    }

    private void deleteChallengeCascade(SaveContext ctx, Challenge challenge, boolean skipTestcaseGuards) {
        List<ClassEntity> classes = ctx.classesByChallengeId.getOrDefault(challenge.getId(), List.of());
        for (ClassEntity classEntity : List.copyOf(classes)) {
            deleteClassCascade(ctx, classEntity, skipTestcaseGuards);
        }
        challengeRepository.delete(challenge);
        ctx.removeChallenge(challenge.getId());
    }

    private void deleteClassCascade(SaveContext ctx, ClassEntity classEntity) {
        deleteClassCascade(ctx, classEntity, false);
    }

    private void deleteClassCascade(SaveContext ctx, ClassEntity classEntity, boolean skipTestcaseGuards) {
        UUID classId = classEntity.getId();
        UUID challengeId = classEntity.getChallenge().getId();
        List<ClassEntity> nestedDependents = ctx.classesByChallengeId.getOrDefault(challengeId, List.of()).stream()
                .filter(candidate -> candidate.getOuterClass() != null
                        && classId.equals(candidate.getOuterClass().getId()))
                .toList();
        for (ClassEntity nested : nestedDependents) {
            deleteClassCascade(ctx, nested, skipTestcaseGuards);
        }
        if (!skipTestcaseGuards) {
            guardTestcaseReference(challengeId, TestcaseRubricService.RubricMemberKind.CLASS, classId);
        }
        deleteRelationsForClass(ctx, classId);
        for (Field field : List.copyOf(ctx.fieldsByClassId.getOrDefault(classId, List.of()))) {
            deleteField(ctx, field, skipTestcaseGuards);
        }
        for (Method method : List.copyOf(ctx.methodsByClassId.getOrDefault(classId, List.of()))) {
            deleteMethod(ctx, method, skipTestcaseGuards);
        }
        for (Constructor constructor : List.copyOf(ctx.constructorsByClassId.getOrDefault(classId, List.of()))) {
            deleteConstructor(ctx, constructor, skipTestcaseGuards);
        }
        classEntityRepository.delete(classEntity);
        ctx.removeClass(classId, challengeId);
    }

    private void deleteField(SaveContext ctx, Field field) {
        deleteField(ctx, field, false);
    }

    private void deleteField(SaveContext ctx, Field field, boolean skipTestcaseGuards) {
        UUID challengeId = field.getClassEntity().getChallenge().getId();
        if (!skipTestcaseGuards) {
            guardTestcaseReference(challengeId, TestcaseRubricService.RubricMemberKind.FIELD, field.getId());
        }
        UUID classId = field.getClassEntity().getId();
        UUID declarationId = field.getFieldDeclaration().getId();
        fieldRepository.delete(field);
        fieldDeclarationRepository.deleteById(declarationId);
        ctx.removeField(field.getId(), classId);
    }

    private void deleteMethod(SaveContext ctx, Method method) {
        deleteMethod(ctx, method, false);
    }

    private void deleteMethod(SaveContext ctx, Method method, boolean skipTestcaseGuards) {
        UUID challengeId = method.getClassEntity().getChallenge().getId();
        if (!skipTestcaseGuards) {
            guardTestcaseReference(challengeId, TestcaseRubricService.RubricMemberKind.METHOD, method.getId());
        }
        UUID classId = method.getClassEntity().getId();
        parameterRepository.deleteByMethod_IdIn(List.of(method.getId()));
        UUID declarationId = method.getMethodDeclaration().getId();
        methodRepository.delete(method);
        methodDeclarationRepository.deleteById(declarationId);
        ctx.removeMethod(method.getId(), classId);
    }

    private void deleteConstructor(SaveContext ctx, Constructor constructor) {
        deleteConstructor(ctx, constructor, false);
    }

    private void deleteConstructor(SaveContext ctx, Constructor constructor, boolean skipTestcaseGuards) {
        UUID challengeId = constructor.getClassEntity().getChallenge().getId();
        if (!skipTestcaseGuards) {
            guardTestcaseReference(challengeId, TestcaseRubricService.RubricMemberKind.CONSTRUCTOR, constructor.getId());
        }
        UUID classId = constructor.getClassEntity().getId();
        parameterRepository.deleteByConstructorEntity_IdIn(List.of(constructor.getId()));
        UUID declarationId = constructor.getConstructorDeclaration().getId();
        constructorRepository.delete(constructor);
        constructorDeclarationRepository.deleteById(declarationId);
        ctx.removeConstructor(constructor.getId(), classId);
    }

    private void guardTestcaseReference(UUID challengeId,
                                        TestcaseRubricService.RubricMemberKind kind,
                                        UUID memberId) {
        List<String> names = testcaseRubricService.findReferencingTestcaseNames(challengeId, kind, memberId);
        if (!names.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot delete: referenced by operational testcase(s): " + String.join(", ", names));
        }
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

    /**
     * Light-save fast path: when the editor re-sends an unchanged challenge tree, skip
     * class/member/relation upsert work (challenge metadata already updated by upsertChallenge).
     */
    private boolean challengeTreeUnchanged(SaveContext ctx, ChallengeStructureDTO payload) {
        Challenge existing = ctx.challengesById.get(payload.id());
        if (existing == null) {
            return false;
        }
        ChallengeStructureDTO current = toChallengeDto(
                existing,
                ctx.classesByChallengeId.getOrDefault(payload.id(), List.of()),
                ctx.fieldsByClassId,
                ctx.methodsByClassId,
                ctx.constructorsByClassId,
                ctx.paramsByMethodId,
                ctx.paramsByConstructorId,
                ctx.relationsByChallengeId.getOrDefault(payload.id(), List.of()));
        return canonicalizeChallenge(current).equals(canonicalizeChallenge(payload));
    }

    private static ChallengeStructureDTO canonicalizeChallenge(ChallengeStructureDTO dto) {
        List<ClassStructureDTO> classes = (dto.classes() != null ? dto.classes() : List.<ClassStructureDTO>of()).stream()
                .sorted(Comparator.comparing(c -> c.id() != null ? c.id().toString() : c.name(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(LabStructureService::canonicalizeClass)
                .toList();
        List<RelationStructureDTO> relations = (dto.relations() != null ? dto.relations() : List.<RelationStructureDTO>of())
                .stream()
                .sorted(Comparator
                        .comparing((RelationStructureDTO r) -> r.id() != null ? r.id().toString() : "")
                        .thenComparing(r -> r.sourceClassId() != null ? r.sourceClassId().toString() : "")
                        .thenComparing(r -> r.targetClassId() != null ? r.targetClassId().toString() : ""))
                .toList();
        return new ChallengeStructureDTO(
                dto.id(),
                dto.name() != null ? dto.name().trim() : null,
                dto.challengeNumber(),
                classes,
                relations,
                dto.hasMmd(),
                normalizeWeight(dto.weight()),
                normalizeWeight(dto.classWeight()),
                normalizeWeight(dto.mmdWeight()),
                normalizeWeight(dto.testcaseWeight()));
    }

    private static ClassStructureDTO canonicalizeClass(ClassStructureDTO dto) {
        List<FieldStructureDTO> fields = (dto.fields() != null ? dto.fields() : List.<FieldStructureDTO>of()).stream()
                .sorted(Comparator.comparing(f -> f.id() != null ? f.id().toString() : f.name(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<MethodStructureDTO> methods = (dto.methods() != null ? dto.methods() : List.<MethodStructureDTO>of()).stream()
                .sorted(Comparator.comparing(m -> m.id() != null ? m.id().toString() : m.name(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(m -> new MethodStructureDTO(
                        m.id(),
                        m.name(),
                        m.returnType(),
                        m.scopeId(),
                        m.isStatic(),
                        m.isAbstract(),
                        canonicalizeParams(m.parameters())))
                .toList();
        List<ConstructorStructureDTO> constructors = (dto.constructors() != null ? dto.constructors()
                : List.<ConstructorStructureDTO>of()).stream()
                .sorted(Comparator.comparing(c -> c.id() != null ? c.id().toString() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .map(c -> new ConstructorStructureDTO(
                        c.id(),
                        c.name(),
                        c.scopeId(),
                        c.isDefault(),
                        canonicalizeParams(c.parameters())))
                .toList();
        return new ClassStructureDTO(
                dto.id(),
                dto.name() != null ? dto.name().trim() : null,
                dto.scopeId(),
                dto.declaringTypeId(),
                dto.isAbstract(),
                dto.isStatic(),
                fields,
                methods,
                constructors,
                dto.outerClassId(),
                normalizeWeight(dto.weight()));
    }

    private static List<ParameterStructureDTO> canonicalizeParams(List<ParameterStructureDTO> params) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }
        return params.stream()
                .sorted(Comparator.comparingInt(ParameterStructureDTO::orderIndex))
                .map(p -> new ParameterStructureDTO(
                        p.id(),
                        p.name() != null ? p.name().trim() : null,
                        p.dataType() != null ? p.dataType().trim() : null,
                        p.orderIndex(),
                        p.isFinal()))
                .toList();
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
                relationDtos,
                challenge.isHasMmd(),
                normalizeWeight(challenge.getWeight()),
                normalizeWeight(challenge.getClassWeight()),
                normalizeWeight(challenge.getMmdWeight()),
                normalizeWeight(challenge.getTestcaseWeight()));
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
                classEntity.isStatic(),
                fieldDtos,
                methodDtos,
                constructorDtos,
                classEntity.getOuterClass() != null ? classEntity.getOuterClass().getId() : null,
                normalizeWeight(classEntity.getWeight()));
    }

    private static int normalizeWeight(int weight) {
        return weight > 0 ? weight : 1;
    }

    private List<ParameterStructureDTO> mapParameters(List<Parameter> parameters) {
        return parameters.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(p -> new ParameterStructureDTO(p.getId(), p.getName(), p.getDataType(), p.getOrderIndex(), p.isFinal()))
                .toList();
    }
}
