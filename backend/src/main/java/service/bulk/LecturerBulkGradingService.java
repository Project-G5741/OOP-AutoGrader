package com.eiu.capstone.backend.service.bulk;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.BulkGradeResponse;
import com.eiu.capstone.backend.grading.GradedChallengeSummary;
import com.eiu.capstone.backend.grading.GradingOutcome;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.plagiarism.PlagiarismFingerprintExtractor;
import com.eiu.capstone.backend.plagiarism.PlagiarismSignals;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.DisclosureMode;
import com.eiu.capstone.backend.service.SubmissionStorageService;
import com.eiu.capstone.backend.utility.TimeUtil;

/**
 * Ephemeral one-student grade for lecturer Bulk Grading.
 * Grades via {@link GradingService} and never calls {@code UploadPersistService},
 * progress UPSERT, plagiarism inspect, or lab-statistics invalidate.
 */
@Service
public class LecturerBulkGradingService {

    private final LabRepository labRepository;
    private final ChallengeRepository challengeRepository;
    private final LabRubricCache labRubricCache;
    private final SubmissionStorageService submissionStorageService;
    private final GradingService gradingService;

    public LecturerBulkGradingService(LabRepository labRepository,
                                      ChallengeRepository challengeRepository,
                                      LabRubricCache labRubricCache,
                                      SubmissionStorageService submissionStorageService,
                                      GradingService gradingService) {
        this.labRepository = labRepository;
        this.challengeRepository = challengeRepository;
        this.labRubricCache = labRubricCache;
        this.submissionStorageService = submissionStorageService;
        this.gradingService = gradingService;
    }

    public BulkGradeResponse gradeOne(UUID labId, BulkGradeMode mode, List<MultipartFile> files) {
        if (labId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "labId is required");
        }
        if (mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required (LAB or EXAM)");
        }

        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));

        List<Challenge> challenges =
                challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
        int challengeCount = challenges.size();
        if (mode == BulkGradeMode.EXAM && challengeCount != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Exam mode requires a lab with exactly 1 challenge (this lab has " + challengeCount + ")");
        }
        if (mode == BulkGradeMode.LAB && challengeCount < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lab mode requires a lab with at least one challenge");
        }

        int examChallengeNumber = 1;
        if (mode == BulkGradeMode.EXAM) {
            Integer number = challenges.get(0).getChallengeNumber();
            if (number == null || number < 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Exam lab challenge number is missing or invalid");
            }
            examChallengeNumber = number;
        }

        List<MultipartFile> remapped = BulkFolderPathRemapper.remap(files, mode, examChallengeNumber);
        String studentFolder = BulkFolderPathRemapper.extractStudentFolder(remapped);
        String irn = BulkFolderPathRemapper.extractIrn(studentFolder);
        String requestId = UUID.randomUUID().toString();

        Path submissionFolderToDelete = null;
        try {
            LabRubricSnapshot rubric = labRubricCache.get(lab);
            SubmissionStorageService.ProcessResult uploadResult =
                    submissionStorageService.processUpload(studentFolder, requestId, remapped);
            submissionFolderToDelete = uploadResult.submissionFolder;

            LabSubmission submission = new LabSubmission();
            submission.setId(UUID.randomUUID());
            submission.setUser(new UserAccount());
            submission.setLab(lab);
            submission.setAttemptNumber(1);
            submission.setScore(BigDecimal.ZERO);
            submission.setSubmittedAt(TimeUtil.nowInVietnam());

            GradingOutcome gradingOutcome = gradingService.gradeSubmission(
                    submission,
                    rubric,
                    uploadResult.challenges,
                    uploadResult.mmdByChallenge,
                    DisclosureMode.LECTURER);

            Map<UUID, Integer> challengeResult = new LinkedHashMap<>();
            for (GradedChallengeSummary graded : gradingOutcome.gradedChallenges()) {
                challengeResult.put(graded.challengeId(), graded.scorePercent());
            }

            PlagiarismSignals signals = PlagiarismFingerprintExtractor.extract(remapped);
            List<String> fileHashes = signals.fileHashes() != null ? signals.fileHashes() : List.of();

            return new BulkGradeResponse(
                    irn,
                    studentFolder,
                    gradingOutcome.overallScore(),
                    challengeResult,
                    gradingOutcome.labResult(),
                    fileHashes);
        } finally {
            if (submissionFolderToDelete != null) {
                submissionStorageService.deleteFolder(submissionFolderToDelete);
            }
        }
    }
}
