package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.*;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassStructureService {

    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final ParameterRepository parameterRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;
    private final MasterDataResolver masterDataResolver;

    public ClassStructureService(ClassEntityRepository classEntityRepository,
                                  FieldRepository fieldRepository,
                                  MethodRepository methodRepository,
                                  ConstructorRepository constructorRepository,
                                  ParameterRepository parameterRepository,
                                  StudentLabProgressRepository studentLabProgressRepository,
                                  SubmissionFieldResultRepository submissionFieldResultRepository,
                                  SubmissionMethodResultRepository submissionMethodResultRepository,
                                  SubmissionConstructorResultRepository submissionConstructorResultRepository,
                                  MasterDataResolver masterDataResolver) {
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.parameterRepository = parameterRepository;
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
        this.masterDataResolver = masterDataResolver;
    }

    /** Powers the "MMD" tab: one box per class, one line per field/constructor/method. */
    public List<MmdClassDTO> getMmdData(UUID labId, UUID challengeId, UUID studentId) {
        UUID submissionId = resolveReferenceSubmissionId(labId, studentId);
        if (submissionId == null) return List.of();

        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challengeId);
        if (classes.isEmpty()) return List.of();

        Set<UUID> correctFieldIds = correctFieldIds(submissionId);
        Set<UUID> correctMethodIds = correctMethodIds(submissionId);
        Set<UUID> correctConstructorIds = correctConstructorIds(submissionId);

        List<MmdClassDTO> result = new ArrayList<>();
        for (ClassEntity ce : classes) {
            List<MmdAttributeDTO> attributes = new ArrayList<>();

            for (Field f : fieldRepository.findByClassEntity_Id(ce.getId())) {
                String label = f.getName() + ": " + f.getFieldDeclaration().getDataType();
                attributes.add(new MmdAttributeDTO(label, "field", correctFieldIds.contains(f.getId())));
            }
            for (Constructor c : constructorRepository.findByClassEntity_Id(ce.getId())) {
                String params = formatParams(parameterRepository.findByConstructorEntity_IdOrderByOrderIndexAsc(c.getId()), false);
                attributes.add(new MmdAttributeDTO(c.getName() + "(" + params + ")", "constructor",
                        correctConstructorIds.contains(c.getId())));
            }
            for (Method m : methodRepository.findByClassEntity_Id(ce.getId())) {
                String label = m.getName() + "(): " + m.getMethodDeclaration().getReturnType();
                attributes.add(new MmdAttributeDTO(label, "method", correctMethodIds.contains(m.getId())));
            }

            result.add(new MmdClassDTO(ce.getName(), attributes));
        }
        return result;
    }

    /** Powers the "Class" tab: one card per class with Fields / Constructors / Methods columns. */
    public List<ClassDetailDTO> getClassData(UUID labId, UUID challengeId, UUID studentId) {
        UUID submissionId = resolveReferenceSubmissionId(labId, studentId);
        if (submissionId == null) return List.of();

        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challengeId);
        if (classes.isEmpty()) return List.of();

        Map<Integer, String> masterData = masterDataResolver.loadAll();

        Set<UUID> correctFieldIds = correctFieldIds(submissionId);
        Set<UUID> correctMethodIds = correctMethodIds(submissionId);
        Set<UUID> correctConstructorIds = correctConstructorIds(submissionId);

        List<ClassDetailDTO> result = new ArrayList<>();
        for (ClassEntity ce : classes) {
            List<ClassFieldDetailDTO> fields = fieldRepository.findByClassEntity_Id(ce.getId()).stream()
                    .map(f -> new ClassFieldDetailDTO(
                            f.getName(),
                            masterData.getOrDefault(f.getFieldDeclaration().getScope(), "-"),
                            f.getFieldDeclaration().getDataType(),
                            correctFieldIds.contains(f.getId())))
                    .toList();

            List<ClassConstructorDetailDTO> constructors = constructorRepository.findByClassEntity_Id(ce.getId()).stream()
                    .map(c -> new ClassConstructorDetailDTO(
                            c.getName(),
                            formatParams(parameterRepository.findByConstructorEntity_IdOrderByOrderIndexAsc(c.getId()), true),
                            correctConstructorIds.contains(c.getId())))
                    .toList();

            List<ClassMethodDetailDTO> methods = methodRepository.findByClassEntity_Id(ce.getId()).stream()
                    .map(m -> new ClassMethodDetailDTO(
                            m.getName(),
                            masterData.getOrDefault(m.getMethodDeclaration().getScope(), "-"),
                            m.getMethodDeclaration().getReturnType(),
                            correctMethodIds.contains(m.getId())))
                    .toList();

            result.add(new ClassDetailDTO(
                    ce.getName(),
                    resolveClassType(ce, masterData),
                    resolveStatus(fields, constructors, methods),
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
                .map(p -> includeType ? (p.getDataType() + " " + p.getName()) : p.getName())
                .collect(Collectors.joining(", "));
    }

    private Set<UUID> correctFieldIds(UUID submissionId) {
        Set<UUID> ids = new HashSet<>();
        for (SubmissionFieldResult r : submissionFieldResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getField().getId());
        }
        return ids;
    }

    private Set<UUID> correctMethodIds(UUID submissionId) {
        Set<UUID> ids = new HashSet<>();
        for (SubmissionMethodResult r : submissionMethodResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getMethod().getId());
        }
        return ids;
    }

    private Set<UUID> correctConstructorIds(UUID submissionId) {
        Set<UUID> ids = new HashSet<>();
        for (SubmissionConstructorResult r : submissionConstructorResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) ids.add(r.getConstructor().getId());
        }
        return ids;
    }

    private UUID resolveReferenceSubmissionId(UUID labId, UUID studentId) {
        if (studentId == null) return null;
        return studentLabProgressRepository.findByUser_IdAndLab_Id(studentId, labId)
                .map(StudentLabProgress::getBestSubmissionId)
                .orElse(null);
    }
}