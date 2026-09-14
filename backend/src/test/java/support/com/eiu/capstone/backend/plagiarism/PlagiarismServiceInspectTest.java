package support.com.eiu.capstone.backend.plagiarism;

import com.eiu.capstone.backend.plagiarism.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionPlagiarismFingerprint;
import com.eiu.capstone.backend.model.SubmissionPlagiarismMatch;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PlagiarismServiceInspectTest {

    private static final String JAVA_SOURCE = "class Copied {}";

    @Mock
    private SubmissionPlagiarismFingerprintRepository fingerprintRepository;
    @Mock
    private SubmissionPlagiarismMatchRepository matchRepository;
    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    private PlagiarismService service;

    private final UUID labId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID submissionId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID peerUserId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private final UUID peerSubmissionId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private final UUID peerUser2Id = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private final UUID peerSubmission2Id = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @BeforeEach
    void setUp() {
        service = new PlagiarismService(
                fingerprintRepository, matchRepository, labSubmissionRepository, new ObjectMapper());
    }

    @Test
    void inspectUpload_emptyPeers_skipsScoreQueries() {
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("80.00"));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of());

        service.inspectUpload(submission, List.of());

        verify(labSubmissionRepository, never()).bestScoreForUserAndLab(any(), any());
        verify(labSubmissionRepository, never()).bestScoreForUserAndLabExcludingSubmission(any(), any(), any());
        verify(labSubmissionRepository, never()).findByLabIdAndUserIdIn(any(), any());
        verify(matchRepository, never()).saveAll(any());
        verify(matchRepository, never()).findByLabId(any());
        verify(matchRepository, never()).findByLabIdAndOtherSubmissionIdIn(any(), any());
        verify(matchRepository).deleteInvolvingSubmission(submissionId);
        verify(fingerprintRepository).save(any());
    }

    @Test
    void inspectUpload_loadsPeerBestsInOneQueryAndSkipsNonMatches() {
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("100.00"));
        SubmissionPlagiarismFingerprint peer = fingerprint(peerSubmissionId, peerUserId, List.of("deadbeef"));
        SubmissionPlagiarismFingerprint peer2 = fingerprint(peerSubmission2Id, peerUser2Id, List.of("cafebabe"));
        List<LabSubmission> attempts = List.of(
                attempt(userId, submissionId, new BigDecimal("100.00"), 2),
                attempt(peerUserId, peerSubmissionId, new BigDecimal("100.00"), 1),
                attempt(peerUser2Id, peerSubmission2Id, new BigDecimal("90.00"), 1));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(peer, peer2));
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any())).thenReturn(attempts);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of());

        MockMultipartFile java = new MockMultipartFile(
                "files", "123_Student/challenge_1/A.java", "text/plain", JAVA_SOURCE.getBytes(StandardCharsets.UTF_8));
        service.inspectUpload(submission, List.of(java));

        verify(labSubmissionRepository).findByLabIdAndUserIdIn(eq(labId), any());
        verify(labSubmissionRepository, never()).bestScoreForUserAndLab(any(), any());
        verify(labSubmissionRepository, never()).bestScoreForUserAndLabExcludingSubmission(any(), any(), any());
        verify(matchRepository, never()).saveAll(any());
        verify(matchRepository, never()).findByLabId(any());
        verify(matchRepository).findByLabIdAndOtherSubmissionIdIn(eq(labId), any());
    }

    @Test
    void inspectUpload_firstCopyOfPeerBest_flagsAndPersistsOnlyContentMatch() {
        String hash = sha256(JAVA_SOURCE);
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("100.00"));
        SubmissionPlagiarismFingerprint copied = fingerprint(peerSubmissionId, peerUserId, List.of(hash));
        SubmissionPlagiarismFingerprint unrelated = fingerprint(peerSubmission2Id, peerUser2Id, List.of("deadbeef"));
        List<LabSubmission> attempts = List.of(
                attempt(userId, submissionId, new BigDecimal("100.00"), 1),
                attempt(peerUserId, peerSubmissionId, new BigDecimal("100.00"), 1),
                attempt(peerUser2Id, peerSubmission2Id, new BigDecimal("40.00"), 1));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(copied, unrelated));
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any())).thenReturn(attempts);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of());

        MockMultipartFile java = new MockMultipartFile(
                "files", "123_Student/challenge_1/A.java", "text/plain", JAVA_SOURCE.getBytes(StandardCharsets.UTF_8));
        service.inspectUpload(submission, List.of(java));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionPlagiarismMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        List<SubmissionPlagiarismMatch> saved = captor.getValue();
        assertEquals(1, saved.size());
        SubmissionPlagiarismMatch match = saved.get(0);
        assertEquals(submissionId, match.getSubmissionId());
        assertEquals(peerSubmissionId, match.getOtherSubmissionId());
        assertTrue(match.isFlagged());
        assertEquals(new BigDecimal("1.00"), match.getHashSimilarity());
    }

    @Test
    void inspectUpload_signalsOverload_flagsWithoutMultipart() {
        String hash = sha256(JAVA_SOURCE);
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("100.00"));
        SubmissionPlagiarismFingerprint copied = fingerprint(peerSubmissionId, peerUserId, List.of(hash));
        List<LabSubmission> attempts = List.of(
                attempt(userId, submissionId, new BigDecimal("100.00"), 1),
                attempt(peerUserId, peerSubmissionId, new BigDecimal("100.00"), 1));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(copied));
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any())).thenReturn(attempts);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of());

        service.inspectUpload(submission, new PlagiarismSignals(List.of(), "", List.of(hash)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionPlagiarismMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().get(0).isFlagged());
        assertEquals(peerSubmissionId, captor.getValue().get(0).getOtherSubmissionId());
    }

    @Test
    void inspectUpload_alreadyProvenAbility_storesContentMatchUnflagged() {
        String hash = sha256(JAVA_SOURCE);
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("100.00"));
        SubmissionPlagiarismFingerprint copied = fingerprint(peerSubmissionId, peerUserId, List.of(hash));
        List<LabSubmission> attempts = List.of(
                attempt(userId, UUID.fromString("88888888-8888-8888-8888-888888888888"), new BigDecimal("100.00"), 1),
                attempt(userId, submissionId, new BigDecimal("100.00"), 2),
                attempt(peerUserId, peerSubmissionId, new BigDecimal("80.00"), 1));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(copied));
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any())).thenReturn(attempts);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of());

        MockMultipartFile java = new MockMultipartFile(
                "files", "123_Student/challenge_1/A.java", "text/plain", JAVA_SOURCE.getBytes(StandardCharsets.UTF_8));
        service.inspectUpload(submission, List.of(java));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionPlagiarismMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        SubmissionPlagiarismMatch match = captor.getValue().get(0);
        assertFalse(match.isFlagged());
        assertEquals(new BigDecimal("1.00"), match.getHashSimilarity());
    }

    @Test
    void inspectUpload_zeroScore_neverFlags() {
        String hash = sha256(JAVA_SOURCE);
        LabSubmission submission = submission(userId, submissionId, BigDecimal.ZERO);
        SubmissionPlagiarismFingerprint copied = fingerprint(peerSubmissionId, peerUserId, List.of(hash));
        List<LabSubmission> attempts = List.of(
                attempt(userId, submissionId, BigDecimal.ZERO, 1),
                attempt(peerUserId, peerSubmissionId, new BigDecimal("100.00"), 1));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(copied));
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any())).thenReturn(attempts);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of());

        MockMultipartFile java = new MockMultipartFile(
                "files", "123_Student/challenge_1/A.java", "text/plain", JAVA_SOURCE.getBytes(StandardCharsets.UTF_8));
        service.inspectUpload(submission, List.of(java));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionPlagiarismMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        assertFalse(captor.getValue().get(0).isFlagged());
    }

    @Test
    void inspectUpload_promotesPeerSideMatchWhenUploaderBestRises() {
        LabSubmission submission = submission(userId, submissionId, new BigDecimal("100.00"));
        SubmissionPlagiarismFingerprint peer = fingerprint(peerSubmissionId, peerUserId, List.of("deadbeef"));
        when(fingerprintRepository.findByLabIdAndUserIdNot(labId, userId)).thenReturn(List.of(peer));

        UUID uploaderPriorId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID copierPriorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        LabSubmission uploaderPrior = attempt(userId, uploaderPriorId, new BigDecimal("40.00"), 1);
        LabSubmission uploaderCurrent = attempt(userId, submissionId, new BigDecimal("100.00"), 2);
        LabSubmission copierPrior = attempt(peerUserId, copierPriorId, new BigDecimal("50.00"), 1);
        LabSubmission copierAttempt = attempt(peerUserId, peerSubmissionId, new BigDecimal("90.00"), 2);
        when(labSubmissionRepository.findByLabIdAndUserIdIn(eq(labId), any()))
                .thenReturn(List.of(uploaderPrior, uploaderCurrent, copierPrior, copierAttempt));

        SubmissionPlagiarismMatch existing = new SubmissionPlagiarismMatch();
        existing.setLabId(labId);
        existing.setSubmissionId(peerSubmissionId);
        existing.setOtherSubmissionId(uploaderPriorId);
        existing.setGitMatch(false);
        existing.setMetadataMatch(false);
        existing.setHashSimilarity(new BigDecimal("1.00"));
        existing.setFlagged(false);
        when(matchRepository.findByLabIdAndOtherSubmissionIdIn(eq(labId), any())).thenReturn(List.of(existing));

        service.inspectUpload(submission, List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionPlagiarismMatch>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchRepository).saveAll(captor.capture());
        List<List<SubmissionPlagiarismMatch>> allSaves = captor.getAllValues();
        SubmissionPlagiarismMatch promoted = allSaves.get(allSaves.size() - 1).get(0);
        assertTrue(promoted.isFlagged());
        assertEquals(peerSubmissionId, promoted.getSubmissionId());
    }

    private LabSubmission submission(UUID ownerId, UUID id, BigDecimal score) {
        Lab lab = org.mockito.Mockito.mock(Lab.class);
        when(lab.getId()).thenReturn(labId);
        UserAccount user = org.mockito.Mockito.mock(UserAccount.class);
        when(user.getId()).thenReturn(ownerId);
        LabSubmission submission = org.mockito.Mockito.mock(LabSubmission.class);
        when(submission.getId()).thenReturn(id);
        when(submission.getLab()).thenReturn(lab);
        when(submission.getUser()).thenReturn(user);
        org.mockito.Mockito.lenient().when(submission.getScore()).thenReturn(score);
        return submission;
    }

    private LabSubmission attempt(UUID ownerId, UUID id, BigDecimal score, int attemptNumber) {
        UserAccount user = org.mockito.Mockito.mock(UserAccount.class);
        org.mockito.Mockito.lenient().when(user.getId()).thenReturn(ownerId);
        LabSubmission attempt = org.mockito.Mockito.mock(LabSubmission.class);
        org.mockito.Mockito.lenient().when(attempt.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(attempt.getUser()).thenReturn(user);
        org.mockito.Mockito.lenient().when(attempt.getScore()).thenReturn(score);
        org.mockito.Mockito.lenient().when(attempt.getAttemptNumber()).thenReturn(attemptNumber);
        return attempt;
    }

    private SubmissionPlagiarismFingerprint fingerprint(UUID otherSubmissionId, UUID otherUserId, List<String> hashes) {
        SubmissionPlagiarismFingerprint fingerprint = new SubmissionPlagiarismFingerprint();
        fingerprint.setSubmissionId(otherSubmissionId);
        fingerprint.setLabId(labId);
        fingerprint.setUserId(otherUserId);
        fingerprint.setGitCommitHashes("[]");
        fingerprint.setMetadataCanonical("");
        try {
            fingerprint.setFileHashes(new ObjectMapper().writeValueAsString(hashes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return fingerprint;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
