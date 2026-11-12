package integration.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.desktop.DesktopLocalUserService;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.AssertionRow;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.TestcaseRow;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseAssertion;
import com.eiu.capstone.backend.model.TestcaseInvocation;
import com.eiu.capstone.backend.model.TestcaseResultStatus;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseAssertionResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.TestcaseAssertionRepository;
import com.eiu.capstone.backend.repository.TestcaseInvocationRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.ClassStructureService;

@SpringBootTest(classes = EiuCapstoneBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("desktop")
class TestcaseReadIntegrationTest {

    private static Path home;

    @DynamicPropertySource
    static void desktopProps(DynamicPropertyRegistry registry) throws Exception {
        home = Files.createTempDirectory("testcase-read-it");
        Files.createDirectories(home.resolve("rubric"));
        registry.add("APP_DESKTOP_HOME", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.install-dir", () -> home.toAbsolutePath().toString());
        registry.add("app.desktop.pack-dir", () -> home.resolve("rubric").toAbsolutePath().toString());
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:testcasereadit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("jwt.secret", () -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Autowired private ClassStructureService classStructureService;
    @Autowired private DesktopLocalUserService desktopLocalUserService;
    @Autowired private MockMvc mockMvc;
    @Autowired private GradingResultJdbcWriter gradingResultJdbcWriter;
    @Autowired private LabRubricCache labRubricCache;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private TermRepository termRepository;
    @Autowired private LabRepository labRepository;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private ClassEntityRepository classEntityRepository;
    @Autowired private MasterDataRepository masterDataRepository;
    @Autowired private TestcaseRepository testcaseRepository;
    @Autowired private TestcaseInvocationRepository testcaseInvocationRepository;
    @Autowired private TestcaseAssertionRepository testcaseAssertionRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private LabSubmissionRepository labSubmissionRepository;
    @Autowired private SubmissionTestcaseResultRepository submissionTestcaseResultRepository;
    @Autowired private SubmissionTestcaseAssertionResultRepository submissionTestcaseAssertionResultRepository;

    private UUID labId;
    private UUID challengeId;
    private UUID studentId;
    private UUID submissionId;

    @BeforeEach
    void seed() {
        submissionTestcaseAssertionResultRepository.deleteAll();
        submissionTestcaseResultRepository.deleteAll();
        labSubmissionRepository.deleteAll();
        testcaseAssertionRepository.deleteAll();
        testcaseInvocationRepository.deleteAll();
        testcaseRepository.deleteAll();
        classEntityRepository.deleteAll();
        challengeRepository.deleteAll();
        labRepository.deleteAll();
        termRepository.deleteAll();
        academicYearRepository.deleteAll();
        userAccountRepository.deleteAll();
        masterDataRepository.deleteAll();
        labRubricCache.invalidateAll();

        MasterData scope = new MasterData();
        scope.setId(1);
        scope.setName("PUBLIC");
        scope.setCategory("SCOPE");
        MasterData declaring = new MasterData();
        declaring.setId(2);
        declaring.setName("CLASS");
        declaring.setCategory("DECLARING_TYPE");
        masterDataRepository.saveAll(List.of(scope, declaring));

        AcademicYear year = new AcademicYear();
        year.setYearLabel("2026-TC-" + UUID.randomUUID().toString().substring(0, 8));
        year = academicYearRepository.save(year);

        Term term = new Term();
        term.setAcademicYear(year);
        term.setTermNumber(1);
        term.setCurrent(true);
        term = termRepository.save(term);

        labId = UUID.randomUUID();
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setName("OT Read IT Lab");
        lab.setTerm(term);
        lab.setStudentVisible(true);
        labRepository.save(lab);

        challengeId = UUID.randomUUID();
        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);
        challenge.setName("Challenge 4");
        challenge.setChallengeNumber(4);
        challengeRepository.save(challenge);

        UUID classId = UUID.randomUUID();
        ClassEntity carClass = new ClassEntity();
        carClass.setId(classId);
        carClass.setName("Shape");
        carClass.setChallenge(challenge);
        carClass.setScope(scope);
        carClass.setDeclaringType(declaring);
        classEntityRepository.save(carClass);

        UUID testcaseId = UUID.randomUUID();
        Testcase ot = new Testcase();
        ot.setId(testcaseId);
        ot.setChallenge(challenge);
        ot.setTestcaseType(TestcaseType.COMPOSITION);
        ot.setName("Circle");
        ot.setOrderIndex(0);
        testcaseRepository.save(ot);

        UUID invocationId = UUID.randomUUID();
        TestcaseInvocation invocation = new TestcaseInvocation();
        invocation.setId(invocationId);
        invocation.setTestcase(ot);
        invocation.setInvocationKind(InvocationKind.CONSTRUCTOR);
        invocation.setDispatchClass(carClass);
        invocation.setParams("[]");
        invocation.setInstanceName("s1");
        invocation.setOrderIndex(0);
        testcaseInvocationRepository.save(invocation);

        UUID assertionId = UUID.randomUUID();
        TestcaseAssertion assertion = new TestcaseAssertion();
        assertion.setId(assertionId);
        assertion.setTestcase(ot);
        assertion.setInvocation(invocation);
        assertion.setAssertionKind(AssertionKind.STDOUT);
        assertion.setExpectedValue("\"ok\"");
        assertion.setComparisonMode(ComparisonMode.EXACT);
        assertion.setOrderIndex(0);
        testcaseAssertionRepository.save(assertion);

        UserAccount student = desktopLocalUserService.ensureLocalStudent(term);
        studentId = student.getId();

        submissionId = UUID.randomUUID();
        LabSubmission submission = new LabSubmission();
        submission.setId(submissionId);
        submission.setUser(student);
        submission.setLab(lab);
        submission.setAttemptNumber(18);
        submission.setScore(new BigDecimal("94.00"));
        labSubmissionRepository.save(submission);

        gradingResultJdbcWriter.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TestcaseRow(
                        testcaseId,
                        TestcaseResultStatus.PASSED,
                        null,
                        "s1 = new Shape()",
                        "\"ok\"",
                        "\"ok\"",
                        List.of(new AssertionRow(assertionId, TestcaseResultStatus.PASSED, "\"ok\"", null))))));
    }

    @Test
    void getTestcaseData_readsPersistedRowsUnderOpenInViewFalse() throws Exception {
        List<TestcaseResultDTO> dtos = classStructureService.getTestcaseData(
                labId, challengeId, studentId, submissionId);
        assertEquals(1, dtos.size());
        TestcaseResultDTO dto = dtos.get(0);
        assertEquals("Circle", dto.getTestcaseName());
        assertEquals("PASS", dto.getResult());
        assertNotNull(dto.getAssertions());
        assertFalse(dto.getAssertions().isEmpty());

        mockMvc.perform(get("/api/labs/" + labId + "/challenges/" + challengeId + "/testcases")
                        .param("submissionId", submissionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testcase_name").value("Circle"))
                .andExpect(jsonPath("$[0].result").value("PASS"))
                .andExpect(jsonPath("$[0].input").value("Shape s1 = new Shape()"))
                .andExpect(jsonPath("$[0].expected_output").value("\"ok\""))
                .andExpect(jsonPath("$[0].actual_output").value("\"ok\""))
                .andExpect(jsonPath("$[0].assertions[0].kind").value("STDOUT"))
                .andExpect(jsonPath("$[0].assertions[0].result").value("PASS"));
    }
}
