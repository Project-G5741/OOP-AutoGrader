package com.eiu.capstone.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionStorageServiceTest {

    @Test
    void acceptsMatchingSubmissionFolderAndChallengeNames() {
        assertTrue(SubmissionStorageService.isValidSubmissionPath(
                "2331200082_Nguyen_Van_A_lab_1/challenge_1/Student.java"));
    }

    @Test
    void rejectsInvalidSubmissionFolderNames() {
        assertFalse(SubmissionStorageService.isValidSubmissionPath(
                "invalid-folder/challenge_1/Student.java"));
    }

    @Test
    void rejectsInvalidChallengeFolderNames() {
        assertFalse(SubmissionStorageService.isValidSubmissionPath(
                "2331200082_Nguyen_Van_A_lab_1/ChallengeA/Student.java"));
    }
}
