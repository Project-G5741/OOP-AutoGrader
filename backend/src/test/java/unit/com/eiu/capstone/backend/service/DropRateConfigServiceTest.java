package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.EggSmashRateUpdateRequest;
import com.eiu.capstone.backend.DTO.GachaRateUpdateRequest;
import com.eiu.capstone.backend.service.EggSmashRateConfig;
import com.eiu.capstone.backend.service.EggSmashRateConfigService;
import com.eiu.capstone.backend.service.GachaRateConfig;
import com.eiu.capstone.backend.service.GachaRateConfigService;

@ExtendWith(MockitoExtension.class)
class DropRateConfigServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private GachaRateConfigService gachaService;
    private EggSmashRateConfigService eggService;

    @BeforeEach
    void setUp() {
        gachaService = new GachaRateConfigService(jdbcTemplate);
        eggService = new EggSmashRateConfigService(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void gachaUpdate_persistsValidRatesAndRejectsCashbackOverflow() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(GachaRateConfig.defaults()));

        GachaRateConfig saved = gachaService.update(new GachaRateUpdateRequest(
                null, null, 80, 60,
                0.005, 0.01, 0.002, 0.02,
                0.10, 0.20, 0.30, 0.30));

        assertEquals(80, saved.hardPityLimit());
        assertEquals(60, saved.softPityStart());
        assertEquals(0.005, saved.directLegendaryRate());
        assertEquals(0.002, saved.pointCrystalRate());
        verify(jdbcTemplate).update(
                anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        ResponseStatusException overflow = assertThrows(
                ResponseStatusException.class,
                () -> gachaService.update(new GachaRateUpdateRequest(
                        100, 900, 100, 90,
                        0.001, 0.002, 0.0001, 0.003,
                        0.40, 0.30, 0.20, 0.20)));
        assertEquals(HttpStatus.BAD_REQUEST, overflow.getStatusCode());

        ResponseStatusException badPity = assertThrows(
                ResponseStatusException.class,
                () -> gachaService.update(new GachaRateUpdateRequest(
                        100, 900, 50, 90,
                        0.001, 0.002, 0.0001, 0.003,
                        0.05, 0.05, 0.10, 0.20)));
        assertEquals(HttpStatus.BAD_REQUEST, badPity.getStatusCode());
    }

    @Test
    void eggSmashUpdate_persistsValidRatesAndRejectsPoolOverflow() {
        EggSmashRateConfig saved = eggService.update(new EggSmashRateUpdateRequest(
                0.02, 0.05, 0.15, 0.50, 1.0,
                0.10, 0.25, 0.40));

        assertEquals(0.02, saved.baseCrystalHitRate());
        assertEquals(0.50, saved.turn8CrystalHitRate());
        assertEquals(0.10, saved.eggRate300());
        verify(jdbcTemplate).update(
                anyString(),
                any(), any(), any(), any(), any(), any(), any(), any());

        ResponseStatusException overflow = assertThrows(
                ResponseStatusException.class,
                () -> eggService.update(new EggSmashRateUpdateRequest(
                        0.0, 0.0001, 0.05, 0.33, 1.0,
                        0.50, 0.40, 0.20)));
        assertEquals(HttpStatus.BAD_REQUEST, overflow.getStatusCode());
    }
}
