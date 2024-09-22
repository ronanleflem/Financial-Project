package finance.project.api.services;

import finance.project.api.controllers.NotFoundException;
import finance.project.api.entities.Symbol;
import finance.project.api.mappers.SymbolMapper;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.SymbolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SymbolServiceJPATest {

    @Mock
    private SymbolRepository symbolRepository;

    @Mock
    private SymbolMapper symbolMapper;

    @InjectMocks
    private SymbolServiceJPA symbolService;

    private Symbol createTestSymbolEntity() {
        return Symbol.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();
    }

    private SymbolDTO createTestSymbolDTO() {
        return SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();
    }

    @Test
    void testCreateSymbol() {
        SymbolDTO symbolDTO = createTestSymbolDTO();
        Symbol symbol = createTestSymbolEntity();

        given(symbolMapper.toEntity(symbolDTO)).willReturn(symbol);
        given(symbolRepository.save(symbol)).willReturn(symbol);
        given(symbolMapper.toDto(symbol)).willReturn(symbolDTO);

        SymbolDTO result = symbolService.createSymbol(symbolDTO);

        assertThat(result).isEqualTo(symbolDTO);
        verify(symbolRepository).save(symbol);
        verify(symbolMapper).toEntity(symbolDTO);
        verify(symbolMapper).toDto(symbol);
    }
    /**
    @Test
    void testUpdateSymbol_successful() {
        UUID id = UUID.randomUUID();
        Symbol existingSymbol = createTestSymbolEntity();
        SymbolDTO updatedSymbolDTO = createTestSymbolDTO();
        updatedSymbolDTO.setName("Updated Name");
        updatedSymbolDTO.setMarket("Updated Market");

        given(symbolRepository.findById(id)).willReturn(Optional.of(existingSymbol));
        given(symbolMapper.toDto(existingSymbol)).willReturn(updatedSymbolDTO);

        SymbolDTO result = symbolService.updateSymbol(id, updatedSymbolDTO);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getMarket()).isEqualTo("Updated Market");
        verify(symbolRepository).save(existingSymbol);
    }
     */

    @Test
    void testUpdateSymbol_notFound() {
        UUID id = UUID.randomUUID();
        SymbolDTO updatedSymbolDTO = createTestSymbolDTO();

        given(symbolRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> symbolService.updateSymbol(id, updatedSymbolDTO));

        verify(symbolRepository, never()).save(any(Symbol.class));
    }

    @Test
    void testDeleteSymbol_successful() {
        UUID id = UUID.randomUUID();
        given(symbolRepository.existsById(id)).willReturn(true);

        boolean result = symbolService.deleteSymbol(id);

        assertThat(result).isTrue();
        verify(symbolRepository).deleteById(id);
    }

    @Test
    void testDeleteSymbol_notFound() {
        UUID id = UUID.randomUUID();
        given(symbolRepository.existsById(id)).willReturn(false);

        boolean result = symbolService.deleteSymbol(id);

        assertThat(result).isFalse();
        verify(symbolRepository, never()).deleteById(id);
    }

    @Test
    void testListAllSymbols() {
        List<Symbol> symbolList = List.of(createTestSymbolEntity());
        List<SymbolDTO> symbolDTOList = List.of(createTestSymbolDTO());

        given(symbolRepository.findAll()).willReturn(symbolList);
        given(symbolMapper.toDto(any(Symbol.class))).willReturn(symbolDTOList.get(0));

        List<SymbolDTO> result = symbolService.listAllSymbols();

        assertThat(result.size()).isEqualTo(1);
        verify(symbolRepository).findAll();
        verify(symbolMapper).toDto(symbolList.get(0));
    }

    @Test
    void testGetSymbolById_successful() {
        UUID id = UUID.randomUUID();
        Symbol symbol = createTestSymbolEntity();
        SymbolDTO symbolDTO = createTestSymbolDTO();

        given(symbolRepository.findById(id)).willReturn(Optional.of(symbol));
        given(symbolMapper.toDto(symbol)).willReturn(symbolDTO);

        Optional<SymbolDTO> result = symbolService.getSymbolById(id);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(symbolDTO);
        verify(symbolRepository).findById(id);
        verify(symbolMapper).toDto(symbol);
    }

    @Test
    void testGetSymbolById_notFound() {
        UUID id = UUID.randomUUID();
        given(symbolRepository.findById(id)).willReturn(Optional.empty());

        Optional<SymbolDTO> result = symbolService.getSymbolById(id);

        assertThat(result).isEmpty();
        verify(symbolRepository).findById(id);
    }

    @Test
    void testGetSymbolByCode_successful() {
        Symbol symbol = createTestSymbolEntity();
        SymbolDTO symbolDTO = createTestSymbolDTO();

        given(symbolRepository.findBySymbol("AAPL")).willReturn(Optional.of(symbol));
        given(symbolMapper.toDto(symbol)).willReturn(symbolDTO);

        SymbolDTO result = symbolService.getSymbolByCode("AAPL");

        assertThat(result).isEqualTo(symbolDTO);
        verify(symbolRepository).findBySymbol("AAPL");
    }

    @Test
    void testGetSymbolByCode_notFound() {
        given(symbolRepository.findBySymbol("AAPL")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> symbolService.getSymbolByCode("AAPL"));

        verify(symbolRepository).findBySymbol("AAPL");
    }
}