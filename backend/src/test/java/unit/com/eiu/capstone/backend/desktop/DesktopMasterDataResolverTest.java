package unit.com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.DesktopMasterDataResolver;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.repository.MasterDataRepository;

class DesktopMasterDataResolverTest {

    @Test
    void resolve_afterRestart_allocatesPastExistingIds() {
        MasterData publicScope = row(1000, "SCOPE", "PUBLIC");
        MasterData privateScope = row(1001, "SCOPE", "PRIVATE");
        MasterDataRepository repo = mock(MasterDataRepository.class);
        when(repo.findAll()).thenReturn(List.of(publicScope, privateScope));
        when(repo.save(any(MasterData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // New resolver instance = process restart with file H2 master_data retained.
        DesktopMasterDataResolver resolver = new DesktopMasterDataResolver(repo);

        Integer compositionId = resolver.relationTypeId("COMPOSITION");

        assertEquals(1002, compositionId);
        assertEquals(1000, resolver.scopeId("PUBLIC"));
        verify(repo).save(argThat(saved ->
                saved.getId() == 1002
                        && "RELATION_TYPE".equals(saved.getCategory())
                        && "COMPOSITION".equals(saved.getName())));
    }

    @Test
    void resolve_emptyDatabase_startsAt1000() {
        MasterDataRepository repo = mock(MasterDataRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        when(repo.save(any(MasterData.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DesktopMasterDataResolver resolver = new DesktopMasterDataResolver(repo);

        assertEquals(1000, resolver.scopeId("PUBLIC"));
        assertEquals(1001, resolver.scopeId("PRIVATE"));
    }

    private static MasterData row(int id, String category, String name) {
        MasterData row = new MasterData();
        row.setId(id);
        row.setCategory(category);
        row.setName(name);
        return row;
    }
}
