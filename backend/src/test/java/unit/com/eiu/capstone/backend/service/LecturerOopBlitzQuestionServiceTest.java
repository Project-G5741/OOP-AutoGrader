package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.OopBlitzBankListResponse;
import com.eiu.capstone.backend.DTO.OopBlitzBankQuestionView;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportRequest;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportResponse;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionUpsertRequest;
import com.eiu.capstone.backend.model.OopBlitzQuestion;
import com.eiu.capstone.backend.repository.OopBlitzQuestionRepository;
import com.eiu.capstone.backend.service.LecturerOopBlitzQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LecturerOopBlitzQuestionServiceTest {

    @Mock
    private OopBlitzQuestionRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID lecturerId = UUID.randomUUID();
    private Map<UUID, OopBlitzQuestion> store;
    private LecturerOopBlitzQuestionService service;

    @BeforeEach
    void setUp() {
        store = new ConcurrentHashMap<>();
        lenient().when(repository.save(any())).thenAnswer(inv -> {
            OopBlitzQuestion q = inv.getArgument(0);
            store.put(q.getId(), q);
            return q;
        });
        lenient().when(repository.saveAll(anyList())).thenAnswer(inv -> {
            List<OopBlitzQuestion> list = inv.getArgument(0);
            list.forEach(q -> store.put(q.getId(), q));
            return list;
        });
        lenient().when(repository.findById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        lenient().when(repository.existsById(any())).thenAnswer(inv -> store.containsKey(inv.getArgument(0)));
        lenient().when(repository.findAllByOrderByUpdatedAtDesc()).thenAnswer(inv -> List.copyOf(store.values()));

        service = new LecturerOopBlitzQuestionService(repository, objectMapper);
    }

    @Test
    void crudLifecycle_createsUpdatesTogglesAndDeletesQuestion() {
        OopBlitzBankQuestionView created = service.create(
                new OopBlitzQuestionUpsertRequest(
                        "output",
                        "medium",
                        "What prints?",
                        List.of("System.out.println(1 + 2);"),
                        List.of("3", "12", "0", "Compile error"),
                        0,
                        "1 + 2 evaluates to 3.",
                        "Basics",
                        true),
                lecturerId);

        assertEquals("OUTPUT", created.kind());
        assertEquals("MEDIUM", created.difficulty());
        assertTrue(created.active());

        OopBlitzBankQuestionView updated = service.update(
                created.id(),
                new OopBlitzQuestionUpsertRequest(
                        "BUG",
                        "EASY",
                        "Click buggy line:",
                        List.of("class A {", "  final int x = 1;", "  void f() { x = 2; }", "}"),
                        List.of(),
                        2,
                        "Line 3: Cannot reassign final field.",
                        "Final",
                        true));
        assertEquals("BUG", updated.kind());
        assertTrue(updated.options().isEmpty());
        assertEquals(2, updated.correctIndex());

        OopBlitzBankQuestionView disabled = service.setActive(created.id(), false);
        assertFalse(disabled.active());

        OopBlitzBankListResponse list = service.listBank();
        assertEquals(1, list.stats().customTotal());
        assertEquals(0, list.stats().customActive());
        assertTrue(list.stats().proceduralTemplateCount() >= 60);
        assertTrue(list.stats().estimatedProceduralVariants() >= 15_000);

        service.delete(created.id());
        verify(repository).deleteById(created.id());
    }

    @Test
    void validation_rejectsOutOfRangeBugLineDuplicateOptionsAndBlankExplanation() {
        ResponseStatusException badBugLine = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new OopBlitzQuestionUpsertRequest(
                                "BUG",
                                "EASY",
                                "Click line:",
                                List.of("int x = 1;", "x = 2;"),
                                List.of(),
                                5,
                                "Line 6 invalid.",
                                null,
                                true),
                        lecturerId));
        assertEquals(HttpStatus.BAD_REQUEST, badBugLine.getStatusCode());

        ResponseStatusException dupOpts = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new OopBlitzQuestionUpsertRequest(
                                "OUTPUT",
                                "EASY",
                                "Output?",
                                List.of("int x = 1;"),
                                List.of("1", "1", "2", "3"),
                                0,
                                "Prints 1.",
                                null,
                                true),
                        lecturerId));
        assertEquals(HttpStatus.BAD_REQUEST, dupOpts.getStatusCode());

        ResponseStatusException blankExplain = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new OopBlitzQuestionUpsertRequest(
                                "UML",
                                "HARD",
                                "UML?",
                                List.of("class A extends B { }"),
                                List.of("B <|-- A", "A <|-- B", "B <|.. A", "B *-- A"),
                                0,
                                "   ",
                                null,
                                true),
                        lecturerId));
        assertEquals(HttpStatus.BAD_REQUEST, blankExplain.getStatusCode());
    }

    @Test
    void bulkImport_importsValidItemsAndReportsInvalidRows() {
        OopBlitzQuestionUpsertRequest valid = new OopBlitzQuestionUpsertRequest(
                "UML",
                "EASY",
                "Pick relation:",
                List.of("class Dog extends Animal { }"),
                List.of("Animal <|-- Dog", "Dog <|-- Animal", "Animal <|.. Dog", "Animal *-- Dog"),
                0,
                "extends maps to <|--.",
                "Inheritance",
                true);
        OopBlitzQuestionUpsertRequest invalid = new OopBlitzQuestionUpsertRequest(
                "OUTPUT",
                "MEDIUM",
                "Missing choices",
                List.of("int a = 1;"),
                List.of("1", "2"),
                0,
                "Only 2 options.",
                null,
                true);

        OopBlitzQuestionImportResponse res = service.bulkImport(
                new OopBlitzQuestionImportRequest(List.of(valid, invalid)),
                lecturerId);

        assertEquals(1, res.importedCount());
        assertEquals(1, res.skippedCount());
        assertEquals(1, res.errors().size());
        assertTrue(res.errors().get(0).startsWith("Item #2:"));
        assertEquals(1, store.size());
    }
}
