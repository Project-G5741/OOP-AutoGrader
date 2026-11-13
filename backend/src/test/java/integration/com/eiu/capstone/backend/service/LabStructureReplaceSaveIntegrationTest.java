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
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.EiuCapstoneBackendApplication;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.FieldDeclaration;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.SubmissionFieldResult;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.Testcase;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorDeclarationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationNameAliasRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationTypeAliasRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationNameAliasRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationReturnTypeAliasRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterNameAliasRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.ParameterTypeAliasRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.repository.TestcaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.LabStructureService;

/**
 * H2 (desktop profile) coverage for replace-aware structure save: OT wipe,
 * stable challenge id (keeps submission and submission_challenge_result),
 * drops per-member grading rows that would block the old rubric delete, class rebuild.
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
    @Autowired private ConstructorRepository constructorRepository;
    @Autowired private ConstructorDeclarationRepository constructorDeclarationRepository;
    @Autowired private MasterDataRepository masterDataRepository;
    @Autowired private TestcaseRepository testcaseRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private LabSubmissionRepository labSubmissionRepository;
    @Autowired private SubmissionChallengeResultRepository submissionChallengeResultRepository;
    @Autowired private SubmissionFieldResultRepository submissionFieldResultRepository;
    @Autowired private FieldRepository fieldRepository;
    @Autowired private FieldDeclarationRepository fieldDeclarationRepository;
    @Autowired private FieldDeclarationNameAliasRepository fieldNameAliasRepository;
    @Autowired private FieldDeclarationTypeAliasRepository fieldTypeAliasRepository;
    @Autowired private MethodRepository methodRepository;
    @Autowired private MethodDeclarationRepository methodDeclarationRepository;
    @Autowired private MethodDeclarationNameAliasRepository methodNameAliasRepository;
    @Autowired private MethodDeclarationReturnTypeAliasRepository methodReturnTypeAliasRepository;
    @Autowired private ParameterRepository parameterRepository;
    @Autowired private ParameterNameAliasRepository parameterNameAliasRepository;
    @Autowired private ParameterTypeAliasRepository parameterTypeAliasRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private UUID labId;
    private UUID challengeId;
    private UUID oldClassId;
    private UUID newClassId;
    private UUID constructorId;
    private UUID testcaseId;
    private UUID termId;
    private int scopeId;
    private int declaringId;

    @BeforeEach
    void seed() {
        parameterNameAliasRepository.deleteAll();
        parameterTypeAliasRepository.deleteAll();
        fieldNameAliasRepository.deleteAll();
        fieldTypeAliasRepository.deleteAll();
        methodNameAliasRepository.deleteAll();
        methodReturnTypeAliasRepository.deleteAll();
        parameterRepository.deleteAll();
        constructorRepository.deleteAll();
        constructorDeclarationRepository.deleteAll();
        methodRepository.deleteAll();
        methodDeclarationRepository.deleteAll();
        submissionFieldResultRepository.deleteAll();
        submissionChallengeResultRepository.deleteAll();
        labSubmissionRepository.deleteAll();
        fieldRepository.deleteAll();
        fieldDeclarationRepository.deleteAll();
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
        constructorId = UUID.randomUUID();
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
                labId, "Replace IT Lab", termId, null, true, null, "Official", List.of(challengeDto));

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

    @Test
    void saveLabStructure_replacedChallenge_dropsFieldGradingRows_keepsSubmission() {
        MasterData scope = masterDataRepository.findById(scopeId).orElseThrow();
        FieldDeclaration declaration = new FieldDeclaration();
        declaration.setName("speed");
        declaration.setDataType("int");
        declaration.setScope(scope);
        declaration = fieldDeclarationRepository.save(declaration);

        UUID fieldId = UUID.randomUUID();
        Field field = new Field();
        field.setId(fieldId);
        field.setName("speed");
        field.setClassEntity(classEntityRepository.findById(oldClassId).orElseThrow());
        field.setFieldDeclaration(declaration);
        fieldRepository.save(field);

        LabSubmission submission = labSubmissionRepository.findAll().get(0);
        SubmissionFieldResult fieldResult = new SubmissionFieldResult();
        fieldResult.setSubmission(submission);
        fieldResult.setField(field);
        fieldResult.setCorrect(true);
        submissionFieldResultRepository.save(fieldResult);

        FieldStructureDTO legacyField = labStructureService.loadForEditor(labId)
                .challenges().get(0).classes().get(0).fields().get(0);
        assertEquals(List.of("speed"), legacyField.names());
        assertEquals(List.of("int"), legacyField.dataTypes());

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
                labId, "Replace IT Lab", termId, null, true, null, "Official", List.of(challengeDto));

        transactionTemplate.executeWithoutResult(status ->
                labStructureService.saveLabStructure(labId, payload, List.of(challengeId)));

        assertEquals(0, submissionFieldResultRepository.count());
        assertFalse(fieldRepository.findById(fieldId).isPresent());
        assertEquals(1, labSubmissionRepository.count());
        assertEquals(1, submissionChallengeResultRepository.count());
        assertTrue(classEntityRepository.findById(newClassId).isPresent());
    }

    @Test
    void saveAndLoadStructure_roundTripsAliasesAndUpsertsParameterByClientId() {
        UUID fieldId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        UUID parameterId = UUID.randomUUID();
        ParameterStructureDTO parameter = new ParameterStructureDTO(
                parameterId, "cost", "double", 0, false,
                List.of("cost", "fee"), List.of("double", "float"));
        FieldStructureDTO field = new FieldStructureDTO(
                fieldId, "cost", "double", scopeId, false, false,
                List.of("cost", "fee", "charge"), List.of("double", "float"));
        MethodStructureDTO method = new MethodStructureDTO(
                methodId, "calculate", "double", scopeId, false, false, List.of(parameter),
                List.of("calculate", "compute"), List.of("double", "float"));

        LabStructureResponse firstPayload = structureWithMembers(field, method);
        transactionTemplate.executeWithoutResult(status ->
                labStructureService.saveLabStructure(labId, firstPayload, List.of(challengeId)));

        LabStructureResponse firstLoad = labStructureService.loadForEditor(labId);
        FieldStructureDTO loadedField = firstLoad.challenges().get(0).classes().get(0).fields().get(0);
        MethodStructureDTO loadedMethod = firstLoad.challenges().get(0).classes().get(0).methods().get(0);
        assertEquals(List.of("cost", "fee", "charge"), loadedField.names());
        assertEquals(List.of("double", "float"), loadedField.dataTypes());
        assertEquals("cost", fieldRepository.findById(fieldId).orElseThrow().getName());
        assertEquals(List.of("calculate", "compute"), loadedMethod.names());
        assertEquals(List.of("double", "float"), loadedMethod.returnTypes());
        assertEquals(parameterId, loadedMethod.parameters().get(0).id());
        assertEquals(List.of("cost", "fee"), loadedMethod.parameters().get(0).names());
        assertEquals("NewCar", firstLoad.challenges().get(0).classes().get(0).constructors().get(0).name());

        FieldStructureDTO reorderedField = new FieldStructureDTO(
                fieldId, "fee", "float", scopeId, false, false,
                List.of("fee", "cost"), List.of("float", "double"));
        ParameterStructureDTO reorderedParameter = new ParameterStructureDTO(
                parameterId, "fee", "float", 0, false,
                List.of("fee"), List.of("float", "double"));
        MethodStructureDTO reorderedMethod = new MethodStructureDTO(
                methodId, "compute", "float", scopeId, false, false, List.of(reorderedParameter),
                List.of("compute"), List.of("float", "double"));

        transactionTemplate.executeWithoutResult(status ->
                labStructureService.saveLabStructure(
                        labId, structureWithMembers(reorderedField, reorderedMethod)));

        LabStructureResponse secondLoad = labStructureService.loadForEditor(labId);
        FieldStructureDTO reloadedField = secondLoad.challenges().get(0).classes().get(0).fields().get(0);
        MethodStructureDTO reloadedMethod = secondLoad.challenges().get(0).classes().get(0).methods().get(0);
        assertEquals(List.of("fee", "cost"), reloadedField.names());
        assertEquals("fee", reloadedField.name());
        assertEquals(List.of("compute"), reloadedMethod.names());
        assertEquals("compute", methodRepository.findById(methodId).orElseThrow().getName());
        assertEquals(parameterId, reloadedMethod.parameters().get(0).id());
        assertEquals(1, parameterRepository.findByMethod_IdOrderByOrderIndexAsc(methodId).size());
    }

    private LabStructureResponse structureWithMembers(
            FieldStructureDTO field, MethodStructureDTO method) {
        ChallengeStructureDTO challenge = new ChallengeStructureDTO(
                challengeId,
                "Challenge 1 rebuilt",
                1,
                List.of(new ClassStructureDTO(
                        newClassId,
                        "NewCar",
                        scopeId,
                        declaringId,
                        false,
                        List.of(field),
                        List.of(method),
                        List.of(new ConstructorStructureDTO(
                                constructorId, "NotAConstructorAlias", scopeId, false, List.of())))),
                List.of());
        return new LabStructureResponse(
                labId, "Replace IT Lab", termId, null, true, null, "Official", List.of(challenge));
    }
}
