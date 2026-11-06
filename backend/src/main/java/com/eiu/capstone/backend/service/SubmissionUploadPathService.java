package com.eiu.capstone.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.exception.SubmissionProcessingException;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabPrefix;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.service.bulk.BulkFolderPathRemapper;
import com.eiu.capstone.backend.service.bulk.BulkGradeMode;

/**
 * Rewrites student uploads for {@link LabPrefix#MIDTERM} labs into the challenge-folder shape
 * expected by {@link SubmissionStorageService} (same as lecturer bulk EXAM mode).
 */
@Service
public class SubmissionUploadPathService {

    private final ChallengeRepository challengeRepository;

    public SubmissionUploadPathService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    public List<MultipartFile> prepareUploadFiles(Lab lab, List<MultipartFile> files) {
        if (lab == null || !LabPrefix.MIDTERM.equals(lab.getPrefix())) {
            return files;
        }
        List<Challenge> challenges =
                challengeRepository.findByLab_IdOrderByChallengeNumberAsc(lab.getId());
        if (challenges.size() != 1) {
            throw new SubmissionProcessingException(
                    "Midterm labs must have exactly one challenge (this lab has " + challenges.size() + ")");
        }
        Integer number = challenges.get(0).getChallengeNumber();
        if (number == null || number < 1) {
            throw new SubmissionProcessingException("Midterm lab challenge number is missing or invalid");
        }
        try {
            return BulkFolderPathRemapper.remap(files, BulkGradeMode.EXAM, number);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST && ex.getReason() != null) {
                throw new SubmissionProcessingException(ex.getReason());
            }
            throw ex;
        }
    }
}
