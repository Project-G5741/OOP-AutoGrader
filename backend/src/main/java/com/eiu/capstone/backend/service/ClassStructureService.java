package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.*;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassStructureService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final SubmissionResultLoader submissionResultLoader;
    private final MasterDataCache masterDataCache;
    private final SubmissionCompileErrorStore compileErrorStore;
    private final boolean timingLog;

    public ClassStructureService(ChallengeRepository challengeRepository,
                                  ClassEntityRepository classEntityRepository,
                                  FieldRepository fieldRepository,
                                  MethodRepository methodRepository,
                                  ConstructorRepository constructorRepository,
                                  ParameterRepository parameterRepository,
                                  SubmissionResolutionService submissionResolutionService,
                                  SubmissionResultLoader submissionResultLoader,
                                  MasterDataCache masterDataCache,
                                  SubmissionCompileErrorStore compileErrorStore,
                                  @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.submissionResultLoader = submissionResultLoader;
        this.masterDataCache = masterDataCache;
        this.compileErrorStore = compileErrorStore;
        this.timingLog = timingLog;
    }

    /**
     * MMD grading is not implemented yet — returns empty until .mmd files are graded.
     */
    public List<MmdClassDTO> getMmdData(UUID labId, UUID challengeId, UUID studentId) {
        return List.of();
    }

    /** Powers the "Class" tab for the student's latest attempt. */
    public List<ClassDetailDTO> getClassData(UUID labId, UUID challengeId, UUID studentId) {
        long start = System.currentTimeMillis();
        UUID submissionId = submissionResolutionService.resolveLatestSubmissionId(labId, studentId);
        if (submissionId == null) {
            return List.of();
        }
        List<ClassDetailDTO> result = buildClassDataForSubmission(submissionId, challengeId);
        if (timingLog) {
            System.out.printf("read_timing class_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }

    public List<ClassDetailDTO> buildClassDataForSubmission(UUID submissionId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            return List.of();
        }

        List<ClassEntity> classes = classEntityRepository.findByChallengeInWithAttributes(List.of(challenge));
        if (classes.isEmpty()) {
            return List.of();
        }

        Map<Integer, String> masterData = masterDataCache.get();
        SubmissionCorrectIds correctIds = submissionResultLoader.loadCorrectIds(submissionId);

        List<Field> allFields = fieldRepository.findByClassEntityInWithDeclaration(classes);
        List<Method> allMethods = methodRepository.findByClassEntityInWithDeclaration(classes);
        List<Constructor> allConstructors = constructorRepository.findByClassEntityInWithDeclaration(classes);
        List<Parameter> constructorParams = allConstructors.isEmpty()
                ? List.of()
                : parameterRepository.findByConstructorEntityIn(allConstructors);

        Map<UUID, List<Field>> fieldsByClass = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<Parameter>> paramsByConstructor = constructorParams.stream()
                .collect(Collectors.groupingBy(p -> p.getConstructorEntity().getId()));

        String compileError = compileErrorStore.get(submissionId, challengeId);

        List<ClassDetailDTO> result = new ArrayList<>();
        for (ClassEntity ce : classes) {
            List<ClassFieldDetailDTO> fields = fieldsByClass.getOrDefault(ce.getId(), List.of()).stream()
                    .map(f -> new ClassFieldDetailDTO(
                            f.getName(),
                            masterData.getOrDefault(f.getFieldDeclaration().getScope(), "-"),
                            f.getFieldDeclaration().getDataType(),
                            correctIds.fieldIds().contains(f.getId())))
                    .toList();

            List<ClassConstructorDetailDTO> constructors = constructorsByClass.getOrDefault(ce.getId(), List.of()).stream()
                    .map(c -> new ClassConstructorDetailDTO(
                            c.getName(),
                            formatParams(paramsByConstructor.getOrDefault(c.getId(), List.of()), true),
                            correctIds.constructorIds().contains(c.getId())))
                    .toList();

            List<ClassMethodDetailDTO> methods = methodsByClass.getOrDefault(ce.getId(), List.of()).stream()
                    .map(m -> new ClassMethodDetailDTO(
                            m.getName(),
                            masterData.getOrDefault(m.getMethodDeclaration().getScope(), "-"),
                            m.getMethodDeclaration().getReturnType(),
                            correctIds.methodIds().contains(m.getId())))
                    .toList();

            result.add(new ClassDetailDTO(
                    ce.getName(),
                    resolveClassType(ce, masterData),
                    compileError != null ? "error" : resolveStatus(fields, constructors, methods),
                    compileError,
                    fields, constructors, methods));
        }
        return result;
    }

    private String resolveClassType(ClassEntity ce, Map<Integer, String> masterData) {
        String declaringType = masterData.getOrDefault(ce.getDeclaringType(), "CLASS");
        return ce.isAbstract() ? "ABSTRACT " + declaringType : declaringType;
    }

    private String resolveStatus(List<ClassFieldDetailDTO> fields,
                                  List<ClassConstructorDetailDTO> constructors,
                                  List<ClassMethodDetailDTO> methods) {
        long total = fields.size() + constructors.size() + methods.size();
        if (total == 0) return "info";

        long correct = fields.stream().filter(ClassFieldDetailDTO::ok).count()
                + constructors.stream().filter(ClassConstructorDetailDTO::ok).count()
                + methods.stream().filter(ClassMethodDetailDTO::ok).count();

        if (correct == total) return "success";
        if (correct == 0) return "error";
        return "warning";
    }

    private String formatParams(List<Parameter> params, boolean includeType) {
        return params.stream()
                .sorted(Comparator.comparingInt(Parameter::getOrderIndex))
                .map(p -> includeType ? (p.getDataType() + " " + p.getName()) : p.getName())
                .collect(Collectors.joining(", "));
    }
}
