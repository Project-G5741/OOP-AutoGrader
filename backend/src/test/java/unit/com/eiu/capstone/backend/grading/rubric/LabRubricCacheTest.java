package unit.com.eiu.capstone.backend.grading.rubric;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricService;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.LabRepository;

@ExtendWith(MockitoExtension.class)
class LabRubricCacheTest {

    @Mock private LabRubricService labRubricService;
    @Mock private LabRepository labRepository;

    private LabRubricCache cache;

    @BeforeEach
    void setUp() {
        cache = new LabRubricCache(labRubricService, labRepository, 30);
    }

    @Test
    void getAll_batchesMisses_andReusesHits() {
        Lab warm = lab("Warm");
        Lab coldA = lab("Cold A");
        Lab coldB = lab("Cold B");

        LabRubricSnapshot warmSnap = new LabRubricSnapshot(warm.getId(), Map.of());
        LabRubricSnapshot coldASnap = new LabRubricSnapshot(coldA.getId(), Map.of());
        LabRubricSnapshot coldBSnap = new LabRubricSnapshot(coldB.getId(), Map.of());

        when(labRubricService.loadForLab(warm)).thenReturn(warmSnap);
        cache.get(warm);

        when(labRubricService.loadForLabs(anyCollection()))
                .thenReturn(Map.of(coldA.getId(), coldASnap, coldB.getId(), coldBSnap));

        Map<UUID, LabRubricSnapshot> loaded = cache.getAll(List.of(warm, coldA, coldB, coldA));

        assertSame(warmSnap, loaded.get(warm.getId()));
        assertSame(coldASnap, loaded.get(coldA.getId()));
        assertSame(coldBSnap, loaded.get(coldB.getId()));
        verify(labRubricService, times(1)).loadForLabs(anyCollection());
        verify(labRubricService, never()).loadForLab(coldA);
        verify(labRubricService, never()).loadForLab(coldB);
    }

    @Test
    void getAll_empty_returnsEmptyWithoutServiceCall() {
        assertTrue(cache.getAll(List.of()).isEmpty());
        verify(labRubricService, never()).loadForLabs(anyCollection());
    }

    @Test
    void getAll_secondCall_isCacheHit() {
        Lab lab = lab("Lab");
        LabRubricSnapshot snap = new LabRubricSnapshot(lab.getId(), Map.of());
        when(labRubricService.loadForLabs(anyCollection())).thenReturn(Map.of(lab.getId(), snap));

        assertSame(snap, cache.getAll(List.of(lab)).get(lab.getId()));
        assertSame(snap, cache.getAll(List.of(lab)).get(lab.getId()));
        verify(labRubricService, times(1)).loadForLabs(anyCollection());
    }

    private static Lab lab(String name) {
        Lab lab = new Lab();
        lab.setId(UUID.randomUUID());
        lab.setName(name);
        return lab;
    }
}
