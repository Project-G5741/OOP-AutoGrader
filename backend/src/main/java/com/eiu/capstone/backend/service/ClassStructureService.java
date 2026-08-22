package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.*;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.testcase.TestcaseResultMapper;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassShellEntry;
import com.eiu.capstone.backend.grading.scoring.PartialCreditEvaluator;
import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassConstructorEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassFieldEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassMethodEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.MmdRelationEntry;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import com.eiu.capstone.backend.utility.TimingLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ClassStructureService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final ClassRelationRepository classRelationRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final SubmissionResultLoader submissionResultLoader;
    private final MasterDataCache masterDataCache;
    private final SubmissionCompileErrorStore compileErrorStore;
    private final SubmissionPackageNormalizationStore packageNormalizationStore;
    private final SubmissionMmdMetaStore submissionMmdMetaStore;
    private final ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;
    private final LabRubricCache labRubricCache;
    private final SubmissionTestcaseResultRepository submissionTestcaseResultRepository;
    private final TestcaseResultMapper testcaseResultMapper;
    private final boolean timingLog;

    public ClassStructureService(ChallengeRepository challengeRepository,
                                  ClassEntityRepository classEntityRepository,
                                  FieldRepository fieldRepository,
                                  MethodRepository methodRepository,
                                  ConstructorRepository constructorRepository,
                                  ParameterRepository parameterRepository,
                                  ClassRelationRepository classRelationRepository,
                                  SubmissionResolutionService submissionResolutionService,
                                  SubmissionResultLoader submissionResultLoader,
                                  MasterDataCache masterDataCache,
                                  SubmissionCompileErrorStore compileErrorStore,
                                  SubmissionPackageNormalizationStore packageNormalizationStore,
                                  SubmissionMmdMetaStore submissionMmdMetaStore,
                                  ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore,
                                  LabRubricCache labRubricCache,
                                  SubmissionTestcaseResultRepository submissionTestcaseResultRepository,
                                  TestcaseResultMapper testcaseResultMapper,
                                  @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.classRelationRepository = classRelationRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.submissionResultLoader = submissionResultLoader;
        this.masterDataCache = masterDataCache;
        this.compileErrorStore = compileErrorStore;
        this.packageNormalizationStore = packageNormalizationStore;
        this.submissionMmdMetaStore = submissionMmdMetaStore;
        this.parsedSubmissionSnapshotStore = parsedSubmissionSnapshotStore;
        this.labRubricCache = labRubricCache;
        this.submissionTestcaseResultRepository = submissionTestcaseResultRepository;
        this.testcaseResultMapper = testcaseResultMapper;
        this.timingLog = timingLog;
    }

    /**
     * Loads rubric structure for many challenges in batched queries (one round-trip per entity type).
     */
    public LabChallengeStructureBundle loadChallengeStructures(Collection<UUID> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return emptyStructureBundle();
        }

        List<Challenge> challenges = challengeRepository.findAllById(challengeIds);
        if (challenges.isEmpty()) {
            return emptyStructureBundle();
        }

        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(challenges);
        if (classes.isEmpty()) {
            return new LabChallengeStructureBundle(
                    masterDataCache.get(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of());
        }

        List<Field> allFields = fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> allMethods = methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Constructor> allConstructors = constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<ClassRelation> allRelations = classRelationRepository.findByClassEntityInWithEndpoints(classes);
        List<Parameter> constructorParams = allConstructors.isEmpty()
                ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);
        List<Parameter> methodParams = allMethods.isEmpty()
                ? List.of()
                : parameterRepository.findByMethodIn(allMethods);

        Map<UUID, List<ClassEntity>> classesByChallengeId = classes.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));
        Map<UUID, List<Field>> fieldsByClassId = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClassId = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClassId = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<Parameter>> paramsByConstructorId = constructorParams.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));
        Map<UUID, List<Parameter>> paramsByMethodId = methodParams.stream()
                .collect(Collectors.groupingBy(p -> p.getMethod().getId()));
        Map<UUID, List<ClassRelation>> relationsBySourceClassId = allRelations.stream()
                .collect(Collectors.groupingBy(r -> r.getClassEntity().getId()));

        return new LabChallengeStructureBundle(
                masterDataCache.get(),
                classesByChallengeId,
                fieldsByClassId,
                methodsByClassId,
                constructorsByClassId,
                paramsByConstructorId,
                paramsByMethodId,
                relationsBySourceClassId);
    }

    private static LabChallengeStructureBundle emptyStructureBundle() {
        return new LabChallengeStructureBundle(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());
    }

    public MmdResponseDTO getMmdData(UUID labId, UUID challengeId, UUID studentId, UUID submissionId) {
        long start = System.currentTimeMillis();
        UUID resolvedSubmissionId = submissionResolutionService.resolveSubmissionId(labId, studentId, submissionId);
        if (resolvedSubmissionId == null) {
            return new MmdResponseDTO(List.of(), null);
        }
        MmdResponseDTO result = buildMmdResponseForSubmission(resolvedSubmissionId, challengeId);
        TimingLog.line(timingLog, "Read MMD", System.currentTimeMillis() - start);
        return result;
    }

    public MmdResponseDTO buildMmdResponseForSubmission(UUID submissionId, UUID challengeId) {
        return buildMmdResponseForSubmission(submissionId, challengeId, null, null);
    }

    public MmdResponseDTO buildMmdResponseForSubmission(UUID submissionId,
                                                        UUID challengeId,
                                                        MmdGradingOutcome mmdOutcome,
                                                        Boolean mmdSubmittedOverride) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return new MmdResponseDTO(List.of(), null);
        }
        LabChallengeStructureBundle structure = loadChallengeStructures(List.of(challengeId));
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);
        ChallengeMmdMeta mmdMeta = submissionMmdMetaStore.get(submissionId, challengeId);
        ChallengeSnapshot snapshot = parsedSubmissionSnapshotStore.get(submissionId, challengeId);
        List<MmdClassDTO> classes = buildMmdData(
                structure,
                challengeId,
                correctIds,
                mmdOutcome,
                mmdSubmittedOverride,
                mmdMeta,
                submissionId,
                snapshot);
        String parseError = mmdMeta != null ? mmdMeta.parseError : null;
        return new MmdResponseDTO(classes, parseError);
    }

    public List<MmdClassDTO> buildMmdDataForSubmission(UUID submissionId, UUID challengeId) {
        return buildMmdDataForSubmission(submissionId, challengeId, null, null);
    }

    public List<MmdClassDTO> buildMmdDataForSubmission(UUID submissionId,
                                                      UUID challengeId,
                                                      MmdGradingOutcome mmdOutcome,
                                                      Boolean mmdSubmittedOverride) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }
        LabChallengeStructureBundle structure = loadChallengeStructures(List.of(challengeId));
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);
        ChallengeMmdMeta mmdMeta = submissionMmdMetaStore.get(submissionId, challengeId);
        ChallengeSnapshot snapshot = parsedSubmissionSnapshotStore.get(submissionId, challengeId);
        return buildMmdData(structure, challengeId, correctIds, mmdOutcome, mmdSubmittedOverride, mmdMeta, submissionId, snapshot);
    }

    public List<MmdClassDTO> buildMmdData(LabChallengeStructureBundle structure,
                                          UUID challengeId,
                                          SubmissionCorrectIds correctIds,
                                          MmdGradingOutcome mmdOutcome,
                                          Boolean mmdSubmittedOverride,
                                          ChallengeMmdMeta mmdMeta,
                                          UUID submissionId) {
        return buildMmdData(structure, challengeId, correctIds, mmdOutcome, mmdSubmittedOverride, mmdMeta, submissionId, null);
    }

    public List<MmdClassDTO> buildMmdData(LabChallengeStructureBundle structure,
                                          UUID challengeId,
                                          SubmissionCorrectIds correctIds,
                                          MmdGradingOutcome mmdOutcome,
                                          Boolean mmdSubmittedOverride,
                                          ChallengeMmdMeta mmdMeta,
                                          UUID submissionId,
                                          ChallengeSnapshot snapshot) {
        List<ClassEntity> classes = structure.classesForChallenge(challengeId);
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> masterData = structure.masterData();
        ChallengeMmdMeta effectiveMeta = mmdMeta != null ? mmdMeta : new ChallengeMmdMeta();
        boolean effectiveMmdSubmitted = mmdSubmittedOverride != null
                ? mmdSubmittedOverride
                : resolveEffectiveMmdSubmitted(submissionId, effectiveMeta, correctIds);

        ParsedSubmissionSnapshot.MmdSnapshot mmdSnapshot = snapshot != null ? snapshot.mmdSnapshot : null;

        List<MmdClassDTO> result = new ArrayList<>();
        for (ClassEntity classEntity : classes) {
            UUID classId = classEntity.getId();
            String classIdStr = classId.toString();
            boolean stereotypeOk;
            if (mmdOutcome != null) {
                stereotypeOk = mmdOutcome.isClassPresent(classId) && mmdOutcome.isClassCorrect(classId);
            } else {
                stereotypeOk = effectiveMeta.classStereotypeCorrect.getOrDefault(classIdStr, false);
                if (!stereotypeOk && effectiveMmdSubmitted && effectiveMeta.classStereotypeCorrect.isEmpty()) {
                    stereotypeOk = classHasMergedCorrectMember(
                            classId,
                            structure.fieldsByClassId(),
                            structure.methodsByClassId(),
                            structure.constructorsByClassId(),
                            correctIds);
                }
            }

            List<MmdAttributeDTO> attributes = new ArrayList<>();
            String stereotypeDisplay = mmdSnapshot != null
                    ? mmdSnapshot.stereotypes.get(classIdStr)
                    : null;
            attributes.add(new MmdAttributeDTO(
                    stereotypeDisplay != null
                            ? stereotypeDisplay
                            : "<<" + resolveClassTypeLabel(classEntity, masterData).toLowerCase() + ">>",
                    "stereotype",
                    stereotypeOk,
                    stereotypeOk ? null : (effectiveMmdSubmitted ? "Class missing from diagram" : "Missing MMD file")));

            structure.fieldsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(field -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isFieldCorrect(field.getId())
                        : correctIds.fieldIds().contains(field.getId());
                String displayName = snapshotAttributeName(mmdSnapshot, field.getId(), formatFieldName(field));
                attributes.add(new MmdAttributeDTO(
                        displayName,
                        "field",
                        ok,
                        ok ? null : "Field mismatch"));
            });

            structure.constructorsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(constructor -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isConstructorCorrect(constructor.getId())
                        : correctIds.constructorIds().contains(constructor.getId());
                String rubricName = formatConstructorName(constructor,
                        structure.paramsByConstructorId().getOrDefault(constructor.getId(), List.of()));
                attributes.add(new MmdAttributeDTO(
                        snapshotAttributeName(mmdSnapshot, constructor.getId(), rubricName),
                        "constructor",
                        ok,
                        ok ? null : "Constructor mismatch"));
            });

            structure.methodsByClassId().getOrDefault(classEntity.getId(), List.of()).forEach(method -> {
                boolean ok = mmdOutcome != null
                        ? mmdOutcome.isMethodCorrect(method.getId())
                        : correctIds.methodIds().contains(method.getId());
                String rubricName = formatMethodName(method,
                        structure.paramsByMethodId().getOrDefault(method.getId(), List.of()));
                attributes.add(new MmdAttributeDTO(
                        snapshotAttributeName(mmdSnapshot, method.getId(), rubricName),
                        "method",
                        ok,
                        ok ? null : "Method mismatch"));
            });

            List<MmdRelationDTO> relations = structure.relationsBySourceClassId()
                    .getOrDefault(classEntity.getId(), List.of()).stream()
                    .map(relation -> {
                        boolean ok = mmdOutcome != null
                                ? mmdOutcome.isRelationCorrect(relation.getId())
                                : correctIds.relationIds().contains(relation.getId());
                        String error = ok
                                ? null
                                : effectiveMeta.relationErrors.getOrDefault(
                                        relation.getId().toString(),
                                        effectiveMmdSubmitted ? "Relation mismatch" : "Missing relationship");
                        MmdRelationEntry relationEntry = mmdSnapshot != null
                                ? mmdSnapshot.relations.get(relation.getId().toString())
                                : null;
                        String from = relationEntry != null
                                ? relationEntry.from
                                : relation.getClassEntity().getName();
                        String to = relationEntry != null
                                ? relationEntry.to
                                : relation.getTargetClassEntity().getName();
                        String relType = relationEntry != null
                                ? relationEntry.relType
                                : MmdComparisonService.displayRelationTypeName(relation.getRelationType().getName());
                        return new MmdRelationDTO(from, to, relType, ok, error);
                    })
                    .toList();

            result.add(new MmdClassDTO(classEntity.getName(), attributes, relations));
        }
        return result;
    }

    /**
     * MMD meta is stored in ephemeral {@code SUBMISSION_BASE_DIR/_mmd_meta}. When that file is
     * missing (deploy wipe, submissions graded before meta existed), infer submission from persisted
     * grading results so the MMD tab does not show "Missing MMD file" for every class.
     */
    private boolean resolveEffectiveMmdSubmitted(UUID submissionId,
                                                 ChallengeMmdMeta mmdMeta,
                                                 SubmissionCorrectIds correctIds) {
        if (mmdMeta.mmdSubmitted) {
            return true;
        }
        if (mmdMeta.parseError != null && !mmdMeta.parseError.isBlank()) {
            return true;
        }
        if (!mmdMeta.relationErrors.isEmpty()) {
            return true;
        }
        if (!mmdMeta.classStereotypeCorrect.isEmpty()) {
            return mmdMeta.mmdSubmitted;
        }
        if (!correctIds.fieldIds().isEmpty()
                || !correctIds.methodIds().isEmpty()
                || !correctIds.constructorIds().isEmpty()
                || !correctIds.relationIds().isEmpty()) {
            return true;
        }
        return submissionResultLoader.hasAnyResults(submissionId);
    }

    private static boolean classHasMergedCorrectMember(
            UUID classId,
            Map<UUID, List<Field>> fieldsByClass,
            Map<UUID, List<Method>> methodsByClass,
            Map<UUID, List<Constructor>> constructorsByClass,
            SubmissionCorrectIds correctIds) {
        return fieldsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(f -> correctIds.fieldIds().contains(f.getId()))
                || methodsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(m -> correctIds.methodIds().contains(m.getId()))
                || constructorsByClass.getOrDefault(classId, List.of()).stream()
                .anyMatch(c -> correctIds.constructorIds().contains(c.getId()));
    }

    private String resolveClassTypeLabel(ClassEntity classEntity, Map<Integer, String> masterData) {
        String declaringType = resolveMasterDataLabel(classEntity.getDeclaringType(), masterData);
        return "-".equals(declaringType) ? "CLASS" : declaringType;
    }

    private String formatFieldName(Field field) {
        return field.getName() + ": " + field.getFieldDeclaration().getDataType();
    }

    private String formatConstructorName(Constructor constructor, List<Parameter> params) {
        String paramList = params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(Parameter::getName)
                .collect(Collectors.joining(", "));
        return constructor.getName() + "(" + paramList + ")";
    }

    private String formatMethodName(Method method, List<Parameter> params) {
        String paramList = params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(Parameter::getName)
                .collect(Collectors.joining(", "));
        return method.getName() + "(" + paramList + ") " + method.getMethodDeclaration().getReturnType();
    }

    private String snapshotAttributeName(ParsedSubmissionSnapshot.MmdSnapshot mmdSnapshot,
                                         UUID elementId,
                                         String rubricFallback) {
        if (mmdSnapshot == null || elementId == null) {
            return rubricFallback;
        }
        return mmdSnapshot.attributes.getOrDefault(elementId.toString(), rubricFallback);
    }

    /** Powers the "Class" tab for the student's latest attempt. */
    public ClassTabResponse getClassData(UUID labId, UUID challengeId, UUID studentId, UUID submissionId) {
        long start = System.currentTimeMillis();
        UUID resolvedSubmissionId = submissionResolutionService.resolveSubmissionId(labId, studentId, submissionId);
        if (resolvedSubmissionId == null) {
            return new ClassTabResponse(List.of(), null);
        }
        List<ClassDetailDTO> result = buildClassDataForSubmission(resolvedSubmissionId, challengeId);
        String notice = packageNormalizationStore.get(resolvedSubmissionId, challengeId);
        TimingLog.line(timingLog, "Read class", System.currentTimeMillis() - start);
        return new ClassTabResponse(result, notice);
    }

    /** Powers the "Operation Test" tab. Returns [] when the student has no reference submission yet. */
    public List<TestcaseResultDTO> getTestcaseData(UUID labId,
                                                   UUID challengeId,
                                                   UUID studentId,
                                                   UUID submissionId) {
        long start = System.currentTimeMillis();
        UUID resolvedSubmissionId = submissionResolutionService.resolveSubmissionId(labId, studentId, submissionId);
        if (resolvedSubmissionId == null) {
            return List.of();
        }
        List<TestcaseResultDTO> result = buildTestcaseDataForSubmission(resolvedSubmissionId, challengeId);
        TimingLog.line(timingLog, "Read testcase", System.currentTimeMillis() - start);
        return result;
    }

    public List<TestcaseResultDTO> buildTestcaseDataForSubmission(UUID submissionId,
                                                                  UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }
        LabRubricSnapshot rubricSnapshot = labRubricCache.get(challenge.getLab());
        ChallengeRubric challengeRubric = rubricSnapshot.byChallengeNumber().get(challenge.getChallengeNumber());
        if (challengeRubric == null) {
            return List.of();
        }

        Map<UUID, SubmissionTestcaseResult> resultsByTestcaseId = submissionTestcaseResultRepository
                .findBySubmission_IdWithTestcase(submissionId)
                .stream()
                .filter(result -> result.getTestcase() != null
                        && challengeId.equals(result.getTestcase().getChallenge().getId()))
                .collect(Collectors.toMap(
                        result -> result.getTestcase().getId(),
                        result -> result,
                        (left, right) -> left,
                        LinkedHashMap::new));

        return testcaseResultMapper.mapChallengeTestcases(
                challengeRubric.testcases(),
                resultsByTestcaseId);
    }

    public List<ClassDetailDTO> buildClassDataForSubmission(UUID submissionId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }
        LabChallengeStructureBundle structure = loadChallengeStructures(List.of(challengeId));
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);
        String compileError = compileErrorStore.get(submissionId, challengeId);
        ChallengeSnapshot snapshot = parsedSubmissionSnapshotStore.get(submissionId, challengeId);
        return buildClassData(structure, challengeId, correctIds, compileError, snapshot);
    }

    public List<ClassDetailDTO> buildClassData(LabChallengeStructureBundle structure,
                                               UUID challengeId,
                                               SubmissionCorrectIds correctIds,
                                               String compileError) {
        return buildClassData(structure, challengeId, correctIds, compileError, null);
    }

    public List<ClassDetailDTO> buildClassData(LabChallengeStructureBundle structure,
                                               UUID challengeId,
                                               SubmissionCorrectIds correctIds,
                                               String compileError,
                                               ChallengeSnapshot snapshot) {
        List<ClassEntity> classes = structure.classesForChallenge(challengeId);
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> masterData = structure.masterData();
        ParsedSubmissionSnapshot.ClassSnapshot classSnapshot = snapshot != null ? snapshot.classSnapshot : null;
        List<ClassDetailDTO> result = new ArrayList<>();
        for (ClassEntity ce : classes) {
            ClassShellEntry shellEntry = classSnapshot != null
                    ? classSnapshot.shells.get(ce.getId().toString())
                    : null;
            String shellStatus = shellEntry != null
                    ? resolveShellStatus(ce, shellEntry, masterData)
                    : null;
            boolean membersGated = compileError != null || "error".equals(shellStatus);

            List<ClassFieldDetailDTO> fields = structure.fieldsByClassId().getOrDefault(ce.getId(), List.of()).stream()
                    .map(f -> {
                        ClassFieldEntry entry = classSnapshot != null
                                ? classSnapshot.fields.get(f.getId().toString())
                                : null;
                        MemberGrade memberGrade = gateMemberGrade(
                                membersGated,
                                resolveFieldGrade(classSnapshot, f, entry, masterData, correctIds));
                        if (entry != null) {
                            return new ClassFieldDetailDTO(
                                    entry.name, entry.scope, entry.dataType, memberGrade.ok(), memberGrade.partial());
                        }
                        return new ClassFieldDetailDTO(
                                f.getName(),
                                resolveMasterDataLabel(f.getFieldDeclaration().getScope(), masterData),
                                f.getFieldDeclaration().getDataType(),
                                memberGrade.ok(),
                                memberGrade.partial());
                    })
                    .toList();

            List<ClassConstructorDetailDTO> constructors = structure.constructorsByClassId()
                    .getOrDefault(ce.getId(), List.of()).stream()
                    .map(c -> {
                        ClassConstructorEntry entry = classSnapshot != null
                                ? classSnapshot.constructors.get(c.getId().toString())
                                : null;
                        MemberGrade memberGrade = gateMemberGrade(
                                membersGated,
                                resolveConstructorGrade(
                                        classSnapshot,
                                        c,
                                        entry,
                                        structure.paramsByConstructorId().getOrDefault(c.getId(), List.of()),
                                        masterData,
                                        correctIds));
                        if (entry != null) {
                            return new ClassConstructorDetailDTO(
                                    entry.name, entry.scope, entry.params, memberGrade.ok(), memberGrade.partial());
                        }
                        return new ClassConstructorDetailDTO(
                                c.getName(),
                                resolveMasterDataLabel(c.getConstructorDeclaration().getScope(), masterData),
                                formatParams(structure.paramsByConstructorId().getOrDefault(c.getId(), List.of()), true),
                                memberGrade.ok(),
                                memberGrade.partial());
                    })
                    .toList();

            List<ClassMethodDetailDTO> methods = structure.methodsByClassId().getOrDefault(ce.getId(), List.of()).stream()
                    .map(m -> {
                        ClassMethodEntry entry = classSnapshot != null
                                ? classSnapshot.methods.get(m.getId().toString())
                                : null;
                        MemberGrade memberGrade = gateMemberGrade(
                                membersGated,
                                resolveMethodGrade(classSnapshot, m, entry, masterData, correctIds));
                        if (entry != null) {
                            return new ClassMethodDetailDTO(
                                    entry.name,
                                    formatMethodModifiers(entry.scope, entry.isStatic, entry.isAbstract, entry.isFinal),
                                    entry.returnType,
                                    memberGrade.ok(),
                                    memberGrade.partial());
                        }
                        MethodDeclaration declaration = m.getMethodDeclaration();
                        return new ClassMethodDetailDTO(
                                m.getName(),
                                formatMethodModifiers(
                                        resolveMasterDataLabel(declaration.getScope(), masterData),
                                        declaration.isStatic(),
                                        declaration.isAbstract(),
                                        declaration.isFinal()),
                                declaration.getReturnType(),
                                memberGrade.ok(),
                                memberGrade.partial());
                    })
                    .toList();

            String displayType = shellEntry != null
                    ? formatStudentClassType(shellEntry)
                    : resolveClassType(ce, masterData);
            String cardStatus;
            if (compileError != null) {
                cardStatus = "error";
            } else if (shellEntry != null) {
                cardStatus = resolveClassCardStatus(shellStatus, fields, constructors, methods);
            } else {
                cardStatus = resolveMemberStatus(fields, constructors, methods);
            }

            result.add(new ClassDetailDTO(
                    formatClassDisplayName(ce),
                    displayType,
                    cardStatus,
                    compileError,
                    fields, constructors, methods));
        }
        return result;
    }

    private String formatClassDisplayName(ClassEntity classEntity) {
        if (classEntity.getOuterClass() == null) {
            return classEntity.getName();
        }
        return classEntity.getOuterClass().getName() + "." + classEntity.getName();
    }

    private String resolveClassType(ClassEntity ce, Map<Integer, String> masterData) {
        String declaringType = resolveClassTypeLabel(ce, masterData);
        return ce.isAbstract() ? "ABSTRACT " + declaringType : declaringType;
    }

    private String formatStudentClassType(ClassShellEntry entry) {
        String declaringType = normalizeDeclaringType(entry.declaringType);
        String label = declaringType != null ? declaringType.toUpperCase(Locale.ROOT) : "CLASS";
        if (entry.isAbstract && !"interface".equals(declaringType) && !"enum".equals(declaringType)) {
            return "ABSTRACT " + label;
        }
        return label;
    }

    private String resolveShellStatus(ClassEntity ce, ClassShellEntry entry, Map<Integer, String> masterData) {
        if (entry == null) {
            return "error";
        }
        return buildShellChecks(ce, entry, masterData).stream().allMatch(Boolean::booleanValue) ? "success" : "error";
    }

    private List<Boolean> buildShellChecks(ClassEntity ce, ClassShellEntry entry, Map<Integer, String> masterData) {
        List<Boolean> checks = new ArrayList<>();
        checks.add(PartialCreditEvaluator.matches(
                resolveMasterDataLabel(ce.getScope(), masterData), entry.scope).get(0));
        checks.add(PartialCreditEvaluator.matches(
                resolveClassTypeLabel(ce, masterData), entry.declaringType).get(0));
        checks.add(ce.isAbstract() == entry.isAbstract);
        if (ce.getOuterClass() != null) {
            checks.add(ce.isStatic() == entry.isStatic);
        }
        return checks;
    }

    private String resolveClassCardStatus(String shellStatus,
                                          List<ClassFieldDetailDTO> fields,
                                          List<ClassConstructorDetailDTO> constructors,
                                          List<ClassMethodDetailDTO> methods) {
        if ("error".equals(shellStatus)) {
            return "error";
        }
        return resolveMemberStatus(fields, constructors, methods);
    }

    private MemberGrade gateMemberGrade(boolean membersGated, MemberGrade memberGrade) {
        return membersGated ? FAILED_MEMBER_GRADE : memberGrade;
    }

    private MemberGrade resolveFieldGrade(ParsedSubmissionSnapshot.ClassSnapshot classSnapshot,
                                           Field field,
                                           ClassFieldEntry entry,
                                           Map<Integer, String> masterData,
                                           SubmissionCorrectIds correctIds) {
        String gradeLabel = classSnapshot != null
                ? classSnapshot.fieldGrades.get(field.getId().toString())
                : null;
        if (gradeLabel != null) {
            return resolveMemberGradeFromLabel(gradeLabel);
        }
        if (entry != null) {
            return memberGradeFromAccuracy(computeFieldAccuracy(field, entry, masterData));
        }
        return new MemberGrade(correctIds.fieldIds().contains(field.getId()), false);
    }

    private MemberGrade resolveMethodGrade(ParsedSubmissionSnapshot.ClassSnapshot classSnapshot,
                                           Method method,
                                           ClassMethodEntry entry,
                                           Map<Integer, String> masterData,
                                           SubmissionCorrectIds correctIds) {
        String gradeLabel = classSnapshot != null
                ? classSnapshot.methodGrades.get(method.getId().toString())
                : null;
        if (gradeLabel != null) {
            return resolveMemberGradeFromLabel(gradeLabel);
        }
        if (entry != null) {
            return memberGradeFromAccuracy(computeMethodAccuracy(method, entry, masterData));
        }
        return new MemberGrade(correctIds.methodIds().contains(method.getId()), false);
    }

    private MemberGrade resolveConstructorGrade(ParsedSubmissionSnapshot.ClassSnapshot classSnapshot,
                                                Constructor constructor,
                                                ClassConstructorEntry entry,
                                                List<Parameter> rubricParams,
                                                Map<Integer, String> masterData,
                                                SubmissionCorrectIds correctIds) {
        String gradeLabel = classSnapshot != null
                ? classSnapshot.constructorGrades.get(constructor.getId().toString())
                : null;
        if (gradeLabel != null) {
            return resolveMemberGradeFromLabel(gradeLabel);
        }
        if (entry != null) {
            return memberGradeFromAccuracy(computeConstructorAccuracy(constructor, entry, rubricParams, masterData));
        }
        return new MemberGrade(correctIds.constructorIds().contains(constructor.getId()), false);
    }

    private double computeFieldAccuracy(Field field, ClassFieldEntry entry, Map<Integer, String> masterData) {
        FieldDeclaration declaration = field.getFieldDeclaration();
        return PartialCreditEvaluator.accuracy(List.of(
                true,
                PartialCreditEvaluator.matches(
                        resolveMasterDataLabel(declaration.getScope(), masterData), entry.scope).get(0),
                PartialCreditEvaluator.matches(declaration.getDataType(), entry.dataType).get(0)));
    }

    private double computeMethodAccuracy(Method method, ClassMethodEntry entry, Map<Integer, String> masterData) {
        MethodDeclaration declaration = method.getMethodDeclaration();
        return PartialCreditEvaluator.accuracy(List.of(
                true,
                PartialCreditEvaluator.matches(
                        resolveMasterDataLabel(declaration.getScope(), masterData), entry.scope).get(0),
                PartialCreditEvaluator.matches(declaration.getReturnType(), entry.returnType).get(0),
                declaration.isStatic() == entry.isStatic,
                declaration.isAbstract() == entry.isAbstract,
                declaration.isFinal() == entry.isFinal));
    }

    private double computeConstructorAccuracy(Constructor constructor,
                                              ClassConstructorEntry entry,
                                              List<Parameter> rubricParams,
                                              Map<Integer, String> masterData) {
        List<String> expectedParams = rubricParams.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(Parameter::getDataType)
                .toList();
        List<String> actualParams = parseSnapshotParamTypes(entry.params);
        boolean defaultMatches = !constructor.getConstructorDeclaration().isDefault()
                || (actualParams.isEmpty() && equalsIgnoreCase("public", entry.scope));
        return PartialCreditEvaluator.accuracy(List.of(
                sameParamTypes(actualParams, expectedParams),
                PartialCreditEvaluator.matches(
                        resolveMasterDataLabel(constructor.getConstructorDeclaration().getScope(), masterData),
                        entry.scope).get(0),
                defaultMatches));
    }

    private List<String> parseSnapshotParamTypes(String params) {
        if (params == null || params.isBlank()) {
            return List.of();
        }
        return Arrays.stream(params.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private boolean sameParamTypes(List<String> actual, List<String> expected) {
        if (actual.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < actual.size(); i++) {
            if (!equalsIgnoreCase(actual.get(i), expected.get(i))) {
                return false;
            }
        }
        return true;
    }

    private MemberGrade memberGradeFromAccuracy(double accuracy) {
        if (accuracy >= 1.0) {
            return new MemberGrade(true, false);
        }
        if (accuracy > 0) {
            return new MemberGrade(false, true);
        }
        return new MemberGrade(false, false);
    }

    private MemberGrade resolveMemberGradeFromLabel(String gradeLabel) {
        return switch (gradeLabel) {
            case "pass" -> new MemberGrade(true, false);
            case "partial" -> new MemberGrade(false, true);
            default -> new MemberGrade(false, false);
        };
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private record MemberGrade(boolean ok, boolean partial) {}

    private static final MemberGrade FAILED_MEMBER_GRADE = new MemberGrade(false, false);

    private String normalizeDeclaringType(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveMasterDataLabel(MasterData masterData, Map<Integer, String> valueMap) {
        if (masterData == null) {
            return "-";
        }
        Integer id = masterData.getId();
        if (id == null) {
            return masterData.getName() != null ? masterData.getName() : "-";
        }
        return valueMap.getOrDefault(id, masterData.getName() != null ? masterData.getName() : "-");
    }

    private String resolveMemberStatus(List<ClassFieldDetailDTO> fields,
                                       List<ClassConstructorDetailDTO> constructors,
                                       List<ClassMethodDetailDTO> methods) {
        long total = fields.size() + constructors.size() + methods.size();
        if (total == 0) {
            return "info";
        }

        long pass = fields.stream().filter(ClassFieldDetailDTO::ok).count()
                + constructors.stream().filter(ClassConstructorDetailDTO::ok).count()
                + methods.stream().filter(ClassMethodDetailDTO::ok).count();
        boolean anyPartial = fields.stream().anyMatch(ClassFieldDetailDTO::partial)
                || constructors.stream().anyMatch(ClassConstructorDetailDTO::partial)
                || methods.stream().anyMatch(ClassMethodDetailDTO::partial);
        long fail = fields.stream().filter(f -> !f.ok() && !f.partial()).count()
                + constructors.stream().filter(c -> !c.ok() && !c.partial()).count()
                + methods.stream().filter(m -> !m.ok() && !m.partial()).count();

        if (pass == total) {
            return "success";
        }
        if (fail == total) {
            return "error";
        }
        if (anyPartial || pass > 0) {
            return "warning";
        }
        return "error";
    }

    private String formatParams(List<Parameter> params, boolean includeType) {
        return params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(p -> includeType ? (p.getDataType() + " " + p.getName()) : p.getName())
                .collect(Collectors.joining(", "));
    }

    static String formatMethodModifiers(String scope, boolean isStatic, boolean isAbstract, boolean isFinal) {
        StringBuilder sb = new StringBuilder();
        if (scope != null && !scope.isBlank() && !"-".equals(scope)) {
            sb.append(scope.trim().toLowerCase(Locale.ROOT));
        }
        if (isStatic) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("static");
        }
        if (isAbstract) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("abstract");
        }
        if (isFinal) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("final");
        }
        return !sb.isEmpty() ? sb.toString() : "-";
    }
}