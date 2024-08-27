package finance.project.api.controllers;

import finance.project.api.entities.Symbol;
import finance.project.api.mappers.SymbolMapper;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.SymbolRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
public class SymbolControllerIT {

    @Autowired
    SymbolController symbolController;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    SymbolMapper symbolMapper;

    private Symbol createAndSaveSymbol(String symbolCode, String name, String market) {
        return symbolRepository.save(Symbol.builder()
                .symbol(symbolCode)
                .name(name)
                .market(market)
                .build());
    }

    @Rollback
    @Transactional
    @Test
    void deleteByIdFound() {
        Symbol symbol = createAndSaveSymbol("AAPL", "Apple Inc.", "NASDAQ");

        ResponseEntity<Void> responseEntity = symbolController.deleteSymbol(symbol.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        assertThat(symbolRepository.findById(symbol.getId()).isEmpty());
    }

    @Rollback
    @Transactional
    @Test
    void updateExistingSymbol() {
        Symbol symbol = createAndSaveSymbol("AAPL", "Apple Inc.", "NASDAQ");

        SymbolDTO symbolDTO = symbolMapper.toDto(symbol);
        symbolDTO.setName("Updated Name");

        ResponseEntity<SymbolDTO> responseEntity = symbolController.updateSymbol(symbol.getId(), symbolDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));

        Symbol updatedSymbol = symbolRepository.findById(symbol.getId()).get();
        assertThat(updatedSymbol.getName()).isEqualTo("Updated Name");
    }

    @Rollback
    @Transactional
    @Test
    void saveNewSymbolTest() {
        SymbolDTO symbolDTO = SymbolDTO.builder()
                .symbol("GOOGL")
                .name("Google LLC")
                .market("NASDAQ")
                .build();

        ResponseEntity<SymbolDTO> responseEntity = symbolController.createSymbol(symbolDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        System.out.println("Location Header: " + responseEntity.getHeaders().getLocation());

        String[] locationParts = responseEntity.getHeaders().getLocation().getPath().split("/");
        UUID savedId = UUID.fromString(locationParts[locationParts.length - 1]);

        Symbol savedSymbol = symbolRepository.findById(savedId).orElse(null);
        assertThat(savedSymbol).isNotNull();
        assertThat(savedSymbol.getSymbol()).isEqualTo(symbolDTO.getSymbol());
        assertThat(savedSymbol.getName()).isEqualTo(symbolDTO.getName());
        assertThat(savedSymbol.getMarket()).isEqualTo(symbolDTO.getMarket());
    }

    @Test
    void testSymbolIdNotFound() {
        assertThrows(NotFoundException.class, () -> {
            symbolController.deleteSymbol(UUID.randomUUID());
        });
    }

    @Test
    void testGetById() {
        Symbol symbol = createAndSaveSymbol("AAPL", "Apple Inc.", "NASDAQ");

        SymbolDTO dto = symbolController.getSymbolById(symbol.getId());

        assertThat(dto).isNotNull();
        assertThat(dto.getSymbol()).isEqualTo("AAPL");
    }

    @Test
    void testListSymbols() {
        createAndSaveSymbol("AAPL", "Apple Inc.", "NASDAQ");
        createAndSaveSymbol("GOOGL", "Google LLC", "NASDAQ");

        List<SymbolDTO> dtos = symbolController.listAllSymbols();

        assertThat(dtos.size()).isEqualTo(5); // Valeur à ajuster si besoin
    }

    @Rollback
    @Transactional
    @Test
    void testEmptyList() {
        symbolRepository.deleteAll();
        List<SymbolDTO> dtos = symbolController.listAllSymbols();

        assertThat(dtos.size()).isEqualTo(0);
    }

    @Rollback
    @Transactional
    @Test
    void saveInvalidSymbolTest() {
        SymbolDTO invalidSymbolDTO = SymbolDTO.builder()
                .symbol("") // Empty symbol should trigger validation error
                .name("Google LLC")
                .market("NASDAQ")
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            symbolController.createSymbol(invalidSymbolDTO);
        });
    }
}