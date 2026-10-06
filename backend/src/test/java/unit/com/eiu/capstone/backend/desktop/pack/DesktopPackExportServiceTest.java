package unit.com.eiu.capstone.backend.desktop.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.desktop.pack.DesktopPackExportService;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSigningKeys;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class DesktopPackExportServiceTest {

    @Mock private TermRepository termRepository;
    @Mock private LabRepository labRepository;
    @Mock private LabRubricCache labRubricCache;
    @Mock private DesktopPackSigningKeys signingKeys;

    private DesktopPackExportService service;

    @BeforeEach
    void setUp() throws Exception {
        // Real serializer; crypto uses embedded key. Signing key is mocked.
        var keyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        when(signingKeys.signingPrivateKey()).thenReturn(Optional.of(keyPair.getPrivate()));
        service = new DesktopPackExportService(
                termRepository,
                labRepository,
                labRubricCache,
                new DesktopPackSerializer(),
                signingKeys);
    }

    @Test
    void exportTermPack_loadsRubricsInOneBatch() {
        UUID termId = UUID.randomUUID();
        Term term = term(termId);
        Lab labA = lab(term, "Alpha");
        Lab labB = lab(term, "Beta");

        when(termRepository.findByIdWithAcademicYear(termId)).thenReturn(Optional.of(term));
        when(labRepository.findByTerm_Id(termId)).thenReturn(List.of(labB, labA));
        when(labRubricCache.getAll(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Iterable<Lab> labs = invocation.getArgument(0);
            Map<UUID, LabRubricSnapshot> map = new java.util.LinkedHashMap<>();
            for (Lab lab : labs) {
                map.put(lab.getId(), new LabRubricSnapshot(lab.getId(), Map.of()));
            }
            return map;
        });

        DesktopPackExportService.DesktopPackDownload download = service.exportTermPack(termId);

        assertTrue(download.filename().startsWith("Rubric_"));
        assertTrue(download.body().length > 0);
        verify(labRubricCache).getAll(anyCollection());
        verify(labRubricCache, never()).get(org.mockito.ArgumentMatchers.any(Lab.class));
        assertEquals("Rubric_2026-2027_Q1.agpack", download.filename());
    }

    private static Term term(UUID termId) {
        AcademicYear year = new AcademicYear();
        year.setYearLabel("2026-2027");
        Term term = new Term();
        term.setId(termId);
        term.setTermNumber(1);
        term.setAcademicYear(year);
        return term;
    }

    private static Lab lab(Term term, String name) {
        Lab lab = new Lab();
        lab.setId(UUID.randomUUID());
        lab.setName(name);
        lab.setTerm(term);
        lab.setStudentVisible(true);
        return lab;
    }
}
