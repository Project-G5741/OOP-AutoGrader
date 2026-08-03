package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.model.*;
import com.eiu.capstone.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ClassEntityRepository classEntityRepository;
    private final FieldRepository fieldRepository;
    private final MethodRepository methodRepository;
    private final ConstructorRepository constructorRepository;
    private final StudentLabProgressRepository studentLabProgressRepository;
    private final SubmissionFieldResultRepository submissionFieldResultRepository;
    private final SubmissionMethodResultRepository submissionMethodResultRepository;
    private final SubmissionConstructorResultRepository submissionConstructorResultRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                             ClassEntityRepository classEntityRepository,
                             FieldRepository fieldRepository,
                             MethodRepository methodRepository,
                             ConstructorRepository constructorRepository,
                             StudentLabProgressRepository studentLabProgressRepository,
                             SubmissionFieldResultRepository submissionFieldResultRepository,
                             SubmissionMethodResultRepository submissionMethodResultRepository,
                             SubmissionConstructorResultRepository submissionConstructorResultRepository) {
        this.challengeRepository = challengeRepository;
        this.classEntityRepository = classEntityRepository;
        this.fieldRepository = fieldRepository;
        this.methodRepository = methodRepository;
        this.constructorRepository = constructorRepository;
        this.studentLabProgressRepository = studentLabProgressRepository;
        this.submissionFieldResultRepository = submissionFieldResultRepository;
        this.submissionMethodResultRepository = submissionMethodResultRepository;
        this.submissionConstructorResultRepository = submissionConstructorResultRepository;
    }

    public List<ChallengeDTO> getChallengesForLab(UUID labId, UUID studentId) {
        List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        UUID referenceSubmissionId = resolveReferenceSubmissionId(labId, studentId);

        List<ChallengeDTO> result = new ArrayList<>();
        for (Challenge challenge : challenges) {
            Integer score = referenceSubmissionId == null
                    ? null
                    : computeChallengeScore(challenge.getId(), referenceSubmissionId);
            result.add(new ChallengeDTO(challenge.getId(), challenge.getName(), score));
        }
        return result;
    }

    /**
     * The submission used to grade the sidebar score (and the MMD/Class
     * tabs, see ClassStructureService) is the student's BEST submission for
     * the lab — student_lab_progress.best_submission_id. Swap this for a
     * "most recent attempt" lookup if you'd rather always show the latest try.
     */
    private UUID resolveReferenceSubmissionId(UUID labId, UUID studentId) {
        if (studentId == null) return null;
        return studentLabProgressRepository.findByUser_IdAndLab_Id(studentId, labId)
                .map(StudentLabProgress::getBestSubmissionId)
                .orElse(null);
    }

    /**
     * There's no single numeric "challenge score" column in the schema —
     * only per-submission is_correct flags per field/method/constructor.
     * This computes the percentage of the challenge's expected members that
     * were graded correct in the reference submission, e.g. "92/100".
     */
    private Integer computeChallengeScore(UUID challengeId, UUID submissionId) {
        List<ClassEntity> classes = classEntityRepository.findByChallenge_Id(challengeId);
        if (classes.isEmpty()) return null;

        List<UUID> classIds = classes.stream().map(ClassEntity::getId).toList();

        List<Field> fields = fieldRepository.findByClassEntity_IdIn(classIds);
        List<Method> methods = methodRepository.findByClassEntity_IdIn(classIds);
        List<Constructor> constructors = constructorRepository.findByClassEntity_IdIn(classIds);

        int total = fields.size() + methods.size() + constructors.size();
        if (total == 0) return null;

        Set<UUID> correctFieldIds = new HashSet<>();
        for (SubmissionFieldResult r : submissionFieldResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) correctFieldIds.add(r.getField().getId());
        }
        Set<UUID> correctMethodIds = new HashSet<>();
        for (SubmissionMethodResult r : submissionMethodResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) correctMethodIds.add(r.getMethod().getId());
        }
        Set<UUID> correctConstructorIds = new HashSet<>();
        for (SubmissionConstructorResult r : submissionConstructorResultRepository.findBySubmission_Id(submissionId)) {
            if (r.isCorrect()) correctConstructorIds.add(r.getConstructor().getId());
        }

        long correct = fields.stream().filter(f -> correctFieldIds.contains(f.getId())).count()
                + methods.stream().filter(m -> correctMethodIds.contains(m.getId())).count()
                + constructors.stream().filter(c -> correctConstructorIds.contains(c.getId())).count();

        return Math.round((float) (correct * 100.0 / total));
    }
}