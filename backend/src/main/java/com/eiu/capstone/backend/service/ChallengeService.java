package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final SubmissionResultLoader submissionResultLoader;
    private final boolean timingLog;

    public ChallengeService(ChallengeRepository challengeRepository,
                             ClassEntityRepository classEntityRepository,
                             FieldRepository fieldRepository,
                             MethodRepository methodRepository,
                             ConstructorRepository constructorRepository,
                             SubmissionResolutionService submissionResolutionService,
                             SubmissionResultLoader submissionResultLoader,
                             @Value("${app.grading.timing-log:false}") boolean timingLog) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.submissionResultLoader = submissionResultLoader;
        this.timingLog = timingLog;
    }

    public List<ChallengeDTO> getChallengesForLab(UUID labId, UUID studentId) {
        long start = System.currentTimeMillis();
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        if (challenges.isEmpty()) {
            return List.of();
        }

        UUID referenceSubmissionId = submissionResolutionService.resolveLatestSubmissionId(labId, studentId);
        SubmissionCorrectIds correctIds = referenceSubmissionId == null
                ? new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of())
                : submissionResultLoader.loadCorrectIds(referenceSubmissionId);

        List<ClassEntity> allClasses = classEntityRepository.findByChallengeInWithAttributes(challenges);
        List<Field> allFields = allClasses.isEmpty()
                ? List.of()
                : fieldRepository.findByClassEntityInWithDeclaration(allClasses);
        List<Method> allMethods = allClasses.isEmpty()
                ? List.of()
                : methodRepository.findByClassEntityInWithDeclaration(allClasses);
        List<Constructor> allConstructors = allClasses.isEmpty()
                ? List.of()
                : constructorRepository.findByClassEntityInWithDeclaration(allClasses);

        Map<UUID, List<Field>> fieldsByClass = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getClassEntity().getId()));
        Map<UUID, List<Method>> methodsByClass = allMethods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassEntity().getId()));
        Map<UUID, List<Constructor>> constructorsByClass = allConstructors.stream()
                .collect(Collectors.groupingBy(c -> c.getClassEntity().getId()));
        Map<UUID, List<ClassEntity>> classesByChallenge = allClasses.stream()
                .collect(Collectors.groupingBy(c -> c.getChallenge().getId()));

        List<ChallengeDTO> result = new ArrayList<>();
        for (Challenge challenge : challenges) {
            Integer score = referenceSubmissionId == null
                    ? null
                    : computeChallengeScore(
                            classesByChallenge.getOrDefault(challenge.getId(), List.of()),
                            fieldsByClass,
                            methodsByClass,
                            constructorsByClass,
                            correctIds);
            result.add(new ChallengeDTO(challenge.getId(), challenge.getName(), score));
        }

        if (timingLog) {
            System.out.printf("read_timing challenges_ms=%d%n", System.currentTimeMillis() - start);
        }
        return result;
    }

    private Integer computeChallengeScore(
                                          List<ClassEntity> classes,
                                          Map<UUID, List<Field>> fieldsByClass,
                                          Map<UUID, List<Method>> methodsByClass,
                                          Map<UUID, List<Constructor>> constructorsByClass,
                                          SubmissionCorrectIds correctIds) {
        if (classes.isEmpty()) {
            return null;
        }

        List<Field> fields = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        List<Constructor> constructors = new ArrayList<>();
        for (ClassEntity classEntity : classes) {
            fields.addAll(fieldsByClass.getOrDefault(classEntity.getId(), List.of()));
            methods.addAll(methodsByClass.getOrDefault(classEntity.getId(), List.of()));
            constructors.addAll(constructorsByClass.getOrDefault(classEntity.getId(), List.of()));
        }

        int total = fields.size() + methods.size() + constructors.size();
        if (total == 0) {
            return null;
        }

        long correct = fields.stream().filter(f -> correctIds.fieldIds().contains(f.getId())).count()
                + methods.stream().filter(m -> correctIds.methodIds().contains(m.getId())).count()
                + constructors.stream().filter(c -> correctIds.constructorIds().contains(c.getId())).count();

        return Math.round((float) (correct * 100.0 / total));
    }
}
