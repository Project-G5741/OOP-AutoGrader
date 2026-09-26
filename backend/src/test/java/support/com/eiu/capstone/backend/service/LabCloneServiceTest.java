package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.CloneSourcesResponse;
import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.service.LabCloneService;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LabCloneServiceTest {

    @Mock
    private LabStructureService labStructureService;
    @Mock
    private TestcaseRubricService testcaseRubricService;
    @Mock
    private LabRepository labRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private RubricCacheInvalidationSupport rubricCacheInvalidationSupport;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    private LabCloneService labCloneService;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        doNothing().when(transactionManager).rollback(any(TransactionStatus.class));
        labCloneService = new LabCloneService(
                labStructureService,
                testcaseRubricService,
                labRepository,
                termRepository,
                rubricCacheInvalidationSupport,
                transactionManager);
    }

    @Test
    void findPreviousCurrentTerm_onlyOneTerm_empty() {
        Term current = term("2025-2026", 1, true);
        when(termRepository.findAllWithAcademicYear()).thenReturn(List.of(current));

        assertTrue(labCloneService.findPreviousCurrentTerm().isEmpty());
    }

    @Test
    void findPreviousCurrentTerm_currentPlusOlder_returnsOlder() {
        Term older = term("2024-2025", 4, false);
        Term current = term("2025-2026", 1, true);
        when(termRepository.findAllWithAcademicYear()).thenReturn(List.of(older, current));

        Optional<Term> previous = labCloneService.findPreviousCurrentTerm();
        assertTrue(previous.isPresent());
        assertEquals(older.getId(), previous.get().getId());
    }

    @Test
    void listCloneSources_noPrevious_empty() {
        Term current = term("2025-2026", 1, true);
        when(termRepository.findAllWithAcademicYear()).thenReturn(List.of(current));

        CloneSourcesResponse response = labCloneService.listCloneSources();
        assertNull(response.sourceTerm());
        assertTrue(response.labs().isEmpty());
    }

    @Test
    void listCloneSources_previousHasLabs() {
        Term older = term("2024-2025", 4, false);
        Term current = term("2025-2026", 1, true);
        Lab lab = new Lab();
        setLabId(lab, UUID.randomUUID());
        lab.setName("Lab A");
        lab.setTerm(older);
        when(termRepository.findAllWithAcademicYear()).thenReturn(List.of(older, current));
        when(labRepository.findByTerm_Id(older.getId())).thenReturn(List.of(lab));

        CloneSourcesResponse response = labCloneService.listCloneSources();
        assertNotNull(response.sourceTerm());
        assertEquals(older.getId(), response.sourceTerm().id());
        assertEquals(1, response.labs().size());
        assertEquals("Lab A", response.labs().get(0).name());
    }

    @Test
    void cloneLabs_rejectsWrongSourceTerm() {
        UUID allowedTermId = UUID.randomUUID();
        UUID otherTermId = UUID.randomUUID();
        UUID targetTermId = UUID.randomUUID();
        UUID sourceLabId = UUID.randomUUID();

        Term otherTerm = new Term();
        setTermId(otherTerm, otherTermId);
        Lab lab = new Lab();
        setLabId(lab, sourceLabId);
        lab.setTerm(otherTerm);

        when(termRepository.existsById(targetTermId)).thenReturn(true);
        when(labRepository.findAllByIdWithTerm(List.of(sourceLabId))).thenReturn(List.of(lab));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> labCloneService.cloneLabs(List.of(sourceLabId), targetTermId, allowedTermId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(labStructureService, never()).createLab(any());
    }

    @Test
    void cloneLab_remapsIdsAndSavesStructureAndOt() {
        UUID sourceLabId = UUID.randomUUID();
        UUID targetTermId = UUID.randomUUID();
        UUID newLabId = UUID.randomUUID();

        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        UUID testcaseId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();

        LabStructureResponse source = new LabStructureResponse(
                sourceLabId,
                "Lab Copy Me",
                UUID.randomUUID(),
                LocalDate.of(2025, 6, 1),
                false,
                LocalDate.of(2025, 1, 1),
                List.of(new ChallengeStructureDTO(
                        challengeId,
                        "Challenge 1",
                        1,
                        List.of(new ClassStructureDTO(
                                classId,
                                "Foo",
                                1,
                                1,
                                false,
                                false,
                                List.of(new FieldStructureDTO(fieldId, "x", "int", 1)),
                                List.of(new MethodStructureDTO(
                                        methodId, "bar", "void", 1, false, false, List.of())),
                                List.of(),
                                null,
                                1)),
                        List.of(new RelationStructureDTO(relationId, classId, classId, 1)),
                        true,
                        1,
                        1,
                        1,
                        1)));

        TestcaseStructureDTO ot = new TestcaseStructureDTO(
                testcaseId,
                "TC1",
                TestcaseType.UNIT,
                TestcaseComparisonMethod.EQUALS,
                1,
                0,
                false,
                null,
                List.of(),
                List.of(new AssertionStructureDTO(
                        assertionId, invocationId, AssertionKind.RETURN_VALUE, null, "1",
                        ComparisonMode.EXACT, 0)),
                List.of(new InvocationStructureDTO(
                        invocationId, InvocationKind.METHOD, null, methodId, "[]",
                        null, null, null, null)),
                null);

        LabStructureResponse createdShell = new LabStructureResponse(
                newLabId, "Lab Copy Me", targetTermId, LocalDate.of(2026, 9, 30),
                true, null, List.of());

        when(labStructureService.loadForEditor(sourceLabId)).thenReturn(source);
        when(testcaseRubricService.loadDtosGroupedByChallengeIds(List.of(challengeId)))
                .thenReturn(Map.of(challengeId, List.of(ot)));
        when(labStructureService.createLab(any(CreateLabRequest.class))).thenReturn(createdShell);
        when(labStructureService.saveLabStructureInsertOnly(eq(newLabId), any())).thenAnswer(inv -> inv.getArgument(1));
        doNothing().when(testcaseRubricService).persistClonedTestcasesBatch(eq(newLabId), any());

        LabStructureResponse result = labCloneService.cloneLab(sourceLabId, targetTermId);

        ArgumentCaptor<CreateLabRequest> createCaptor = ArgumentCaptor.forClass(CreateLabRequest.class);
        verify(labStructureService).createLab(createCaptor.capture());
        assertEquals("Lab Copy Me", createCaptor.getValue().name());
        assertEquals(targetTermId, createCaptor.getValue().termId());
        assertNull(createCaptor.getValue().deadlineDate());

        ArgumentCaptor<LabStructureResponse> structureCaptor =
                ArgumentCaptor.forClass(LabStructureResponse.class);
        verify(labStructureService).saveLabStructureInsertOnly(eq(newLabId), structureCaptor.capture());
        LabStructureResponse saved = structureCaptor.getValue();
        assertEquals(newLabId, saved.id());
        assertEquals(1, saved.challenges().size());
        ChallengeStructureDTO savedChallenge = saved.challenges().get(0);
        assertNotEquals(challengeId, savedChallenge.id());
        assertEquals("Challenge 1", savedChallenge.name());
        ClassStructureDTO savedClass = savedChallenge.classes().get(0);
        assertNotEquals(classId, savedClass.id());
        assertNotEquals(fieldId, savedClass.fields().get(0).id());
        assertNotEquals(methodId, savedClass.methods().get(0).id());
        RelationStructureDTO savedRelation = savedChallenge.relations().get(0);
        assertNotEquals(relationId, savedRelation.id());
        assertEquals(savedClass.id(), savedRelation.sourceClassId());
        assertEquals(savedClass.id(), savedRelation.targetClassId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<UUID, List<TestcaseStructureDTO>>> otCaptor = ArgumentCaptor.forClass(Map.class);
        verify(testcaseRubricService).persistClonedTestcasesBatch(eq(newLabId), otCaptor.capture());
        Map<UUID, List<TestcaseStructureDTO>> otByChallenge = otCaptor.getValue();
        assertEquals(1, otByChallenge.size());
        assertTrue(otByChallenge.containsKey(savedChallenge.id()));
        TestcaseStructureDTO savedOt = otByChallenge.get(savedChallenge.id()).get(0);
        assertNotEquals(testcaseId, savedOt.id());
        assertNotEquals(invocationId, savedOt.invocations().get(0).id());
        assertEquals(savedClass.methods().get(0).id(), savedOt.invocations().get(0).methodId());
        assertNotEquals(assertionId, savedOt.assertions().get(0).id());
        assertEquals(savedOt.invocations().get(0).id(), savedOt.assertions().get(0).invocationId());

        assertEquals(newLabId, result.id());
        assertEquals("Lab Copy Me", result.name());
        verify(rubricCacheInvalidationSupport).invalidateLab(newLabId);
    }

    @Test
    void cloneLabs_successReturnsCreated() {
        UUID allowedTermId = UUID.randomUUID();
        UUID targetTermId = UUID.randomUUID();
        UUID sourceLabId = UUID.randomUUID();
        UUID newLabId = UUID.randomUUID();

        Term allowedTerm = new Term();
        setTermId(allowedTerm, allowedTermId);
        Lab lab = new Lab();
        setLabId(lab, sourceLabId);
        lab.setName("Source");
        lab.setTerm(allowedTerm);

        LabStructureResponse emptySource = new LabStructureResponse(
                sourceLabId, "Source", allowedTermId, null, true, null, List.of());
        LabStructureResponse createdShell = new LabStructureResponse(
                newLabId, "Source", targetTermId, null, true, null, List.of());

        when(termRepository.existsById(targetTermId)).thenReturn(true);
        when(labRepository.findAllByIdWithTerm(List.of(sourceLabId))).thenReturn(List.of(lab));
        when(labStructureService.loadForEditor(sourceLabId)).thenReturn(emptySource);
        when(labStructureService.createLab(any())).thenReturn(createdShell);
        when(labStructureService.saveLabStructureInsertOnly(eq(newLabId), any())).thenAnswer(inv -> inv.getArgument(1));

        CloneLabsResponse response = labCloneService.cloneLabs(
                List.of(sourceLabId), targetTermId, allowedTermId);

        assertEquals(1, response.created().size());
        assertEquals(newLabId, response.created().get(0).id());
        assertTrue(response.errors().isEmpty());
    }

    private static Term term(String yearLabel, int termNumber, boolean current) {
        AcademicYear year = new AcademicYear();
        year.setYearLabel(yearLabel);
        Term term = new Term();
        setTermId(term, UUID.randomUUID());
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        term.setCurrent(current);
        return term;
    }

    private static void setTermId(Term term, UUID id) {
        try {
            var field = Term.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(term, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setLabId(Lab lab, UUID id) {
        try {
            var field = Lab.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(lab, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
