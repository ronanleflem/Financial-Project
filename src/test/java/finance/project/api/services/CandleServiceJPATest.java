package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CandleServiceJPATest {

    @Mock
    private CandleRepository candleRepository;

    @Mock
    private SymbolRepository symbolRepository;

    @Mock
    private CandleMapper candleMapper;

    @InjectMocks
    private CandleServiceJPA candleServiceJPA;

    private SymbolDTO symbolDTO;
    private Symbol symbolEntity;
    private Candle candleEntity;
    private CandleDTO candleDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup initial test data
        symbolDTO = SymbolDTO.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();

        symbolEntity = Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();

        candleEntity = Candle.builder()
                .id(UUID.randomUUID())
                .symbol(symbolEntity)
                .date(LocalDate.now())
                .open(BigDecimal.valueOf(100.00))
                .close(BigDecimal.valueOf(110.00))
                .high(BigDecimal.valueOf(115.00))
                .low(BigDecimal.valueOf(95.00))
                .volume(BigDecimal.valueOf(1000000))
                .build();

        candleDTO = CandleDTO.builder()
                .id(candleEntity.getId())
                .symbol(symbolEntity)
                .date(candleEntity.getDate())
                .open(candleEntity.getOpen())
                .close(candleEntity.getClose())
                .high(candleEntity.getHigh())
                .low(candleEntity.getLow())
                .volume(candleEntity.getVolume())
                .build();
    }

    @Test
    void testGetCandlesWithValidSymbolAndCandles() {
        when(symbolRepository.findBySymbol(symbolDTO.getSymbol())).thenReturn(Optional.of(symbolEntity));
        when(candleRepository.findBySymbolAndDateBetween(any(Symbol.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(candleEntity));
        when(candleMapper.toDto(any(Candle.class))).thenReturn(candleDTO);

        List<CandleDTO> result = candleServiceJPA.getCandles(symbolDTO, "daily");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol().getSymbol());

        verify(symbolRepository, times(1)).findBySymbol(symbolDTO.getSymbol());
        verify(candleRepository, times(1)).findBySymbolAndDateBetween(any(Symbol.class), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGetCandlesWithValidSymbolButNoCandles() {
        when(symbolRepository.findBySymbol(symbolDTO.getSymbol())).thenReturn(Optional.of(symbolEntity));
        when(candleRepository.findBySymbolAndDateBetween(any(Symbol.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<CandleDTO> result = candleServiceJPA.getCandles(symbolDTO, "daily");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(symbolRepository, times(1)).findBySymbol(symbolDTO.getSymbol());
        verify(candleRepository, times(1)).findBySymbolAndDateBetween(any(Symbol.class), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void testGetCandlesWithInvalidSymbol() {
        when(symbolRepository.findBySymbol(symbolDTO.getSymbol())).thenReturn(Optional.empty());

        List<CandleDTO> result = candleServiceJPA.getCandles(symbolDTO, "daily");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(symbolRepository, times(1)).findBySymbol(symbolDTO.getSymbol());
        verify(candleRepository, never()).findBySymbolAndDateBetween(any(Symbol.class), any(LocalDate.class), any(LocalDate.class));
    }
}
