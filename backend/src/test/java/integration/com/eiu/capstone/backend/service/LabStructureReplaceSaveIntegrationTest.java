package integration.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.LabStructureService;

/**
 * H2 (desktop profile) coverage for replace-aware structure save: OT wipe,
 * stable challenge id (keeps submission_challenge_result FK), class rebuild.
 */
@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@ActiveProfiles("desktop")
class LabStructureReplaceSaveIntegrationTest {

    private static Path home;

    @DynamicPropertySource
    static void desktopProps(DynamicPropertyRegistry registry) throws Exception {
        home = Files.createTempDirectory("lab-structure-replace-it");
        Files.createDirectories(home.resolve("rubric"));
        registry.add("APP_DESKTOP_HOME", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.install-dir", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.pack-dir", () -> home.resolve("rubric").toAbsolutePath().toString());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:labstructreplace;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Autowired private LabStructureService labStructureService;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private TermRepository termRepository;
    @Autowired private LabRepository labRepository;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private ClassEntityRepository classEntityRepository;
    @Autowired private MasterDataRepository masterDataRepository;
    @Autowired private TestcaseRepository testcaseRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private LabSubmissionRepository labSubmissionRepository;
    @Autowired private SubmissionChallengeResultRepository submissionChallengeResultRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private UUID labId;
    private UUID challengeId;
    private UUID oldClassId;
    private UUID newClassId;
    private UUID testcaseId;
    private UUID termId;
    private int scopeId;
    private int declaringId;

    @BeforeEach
    void seed() {
        submissionChallengeResultRepository.deleteAll();
        labSubmissionRepository.deleteAll();
        testcaseRepository.deleteAll();
        classEntityRepository.deleteAll();
        challengeRepository.deleteAll();
        labRepository.deleteAll();
        termRepository.deleteAll();
        academicYearRepository.deleteAll();
        userAccountRepository.deleteAll();
        masterDataRepository.deleteAll();

        MasterData scope = new MasterData();
        scope.setId(1);
        scope.setName("PUBLIC");
        scope.setCategory("SCOPE");
        MasterData declaring = new MasterData();
        declaring.setId(2);
        declaring.setName("CLASS");
        declaring.setCategory("DECLARING_TYPE");
        masterDataRepository.saveAll(List.of(scope, declaring));
        scopeId = 1;
        declaringId = 2;

        AcademicYear year = new AcademicYear();
        year.setYearLabel("2026-IT-" + UUID.randomUUID().toString().substring(0, 8));
        year = academicYearRepository.save(year);

        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(1);
        term.setCurrent(true);
        term = termRepository.save(term);
        termId = term.getId();

        labId = UUID.randomUUID();
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setName("Replace IT Lab");
        lab.setTerm(term);
        lab.setStudentVisible(true);
        labRepository.save(lab);

        challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);
        challenge.setName("Challenge 1");
        challenge.setChallengeNumber(1);
        challengeRepository.save(challenge);

        oldClassId = UUID.randomUUID();
        ClassEntity oldClass = new ClassEntity();
        oldClass.setId(oldClassId);
        oldClass.setName("OldCar");
        oldClass.setChallenge(challenge);
        oldClass.setScope(scope);
        oldClass.setDeclaringType(declaring);
        classEntityRepository.save(oldClass);

        testcaseId = UUID.randomUUID();
        Testcase ot = new Testcase();
        ot.setId(testcaseId);
        ot.setChallenge(challenge);
        ot.setTestcaseType(TestcaseType.UNIT);
        ot.setName("unit-1");
        ot.setOrderIndex(0);
        testcaseRepository.save(ot);

        UserAccount student = new UserAccount();
        student.setFullName("IT Student");
        student.setEmail("replace-it-" + UUID.randomUUID() + "@eiu.edu.vn");
        student.setPasswordHash("x");
        student.setStudentCode("IT" + UUID.randomUUID().toString().substring(0, 8));
        student.setIsActive(true);
        student = userAccountRepository.save(student);

        LabSubmission submission = new LabSubmission();
        submission.setId(UUID.randomUUID());
        submission.setUser(student);
        submission.setLab(lab);
        submission.setAttemptNumber(1);
        submission.setScore(BigDecimal.TEN);
        labSubmissionRepository.save(submission);

        SubmissionChallengeResult scr = new SubmissionChallengeResult();
        scr.setSubmission(submission);
        scr.setChallenge(challenge);
        scr.setCorrect(true);
        scr.setScore(BigDecimal.TEN);
        submissionChallengeResultRepository.save(scr);

        newClassId = UUID.randomUUID();
    }

    @Test
    void saveLabStructure_replacedChallengeIds_wipesOtKeepsChallengeAndScr_rebuildsClasses() {
        ChallengeStructureDTO challengeDto = new ChallengeStructureDTO(
                challengeId,
                "Challenge 1 rebuilt",
                1,
                List.of(new ClassStructureDTO(
                        newClassId,
                        "NewCar",
                        scopeId,
                        declaringId,
                        false,
                        List.of(),
                        List.of(),
                        List.of())),
                List.of());
        LabStructureResponse payload = new LabStructureResponse(
                labId, "Replace IT Lab", termId, null, true, null, List.of(challengeDto));

        LabStructureResponse saved = transactionTemplate.execute(status ->
                labStructureService.saveLabStructure(labId, payload, List.of(challengeId)));

        assertEquals(labId, saved.id());
        assertEquals(challengeId, saved.challenges().get(0).id());
        assertEquals("Challenge 1 rebuilt", saved.challenges().get(0).name());

        assertTrue(challengeRepository.findById(challengeId).isPresent());
        assertFalse(classEntityRepository.findById(oldClassId).isPresent());
        assertTrue(classEntityRepository.findById(newClassId).isPresent());
        assertEquals("NewCar", classEntityRepository.findById(newClassId).orElseThrow().getName());
        assertTrue(testcaseRepository.findByChallenge_IdOrderByOrderIndexAsc(challengeId).isEmpty());
        assertEquals(1, submissionChallengeResultRepository.findAll().stream()
                .filter(row -> challengeId.equals(row.getChallenge().getId()))
                .count());
    }
}
