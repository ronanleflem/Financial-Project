package finance.project.api.services;

import finance.project.api.entities.Symbol;
import finance.project.api.model.SymbolDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SymbolServiceImpl implements SymbolService {

    private Map<UUID, SymbolDTO> symbolMap;

    public SymbolServiceImpl() {
        SymbolDTO symbol1 = SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .name("Apple Inc.")
                .market("NASDAQ")
                .build();

        SymbolDTO symbol2 = SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol("EURUSD")
                .name("Euro to US Dollar")
                .market("Forex")
                .build();

        SymbolDTO symbol3 = SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol("GOLD")
                .name("Gold Futures")
                .market("Commodities")
                .build();
        symbolMap = new HashMap<>();
        symbolMap.put(symbol1.getId(),symbol1);
        symbolMap.put(symbol2.getId(),symbol2);
        symbolMap.put(symbol3.getId(),symbol3);
    }
    @Override
    public SymbolDTO createSymbol(SymbolDTO symbolDTO) {
        symbolDTO.setId(UUID.randomUUID());
        symbolMap.put(symbolDTO.getId(), symbolDTO);
        return symbolDTO;
    }

    @Override
    public SymbolDTO updateSymbol(UUID id, SymbolDTO updatedSymbolDTO) {
        SymbolDTO existingSymbol = symbolMap.get(id);
        if (existingSymbol == null) {
            throw new RuntimeException("Symbol not found");
        }
        updatedSymbolDTO.setId(id);
        symbolMap.put(id, updatedSymbolDTO);
        return updatedSymbolDTO;
    }

    @Override
    public Boolean deleteSymbol(UUID id) {
        symbolMap.remove(id);
        return true;
    }

    @Override
    public List<SymbolDTO> listAllSymbols() {
        return new ArrayList<>(symbolMap.values());
    }

    @Override
    public Optional<SymbolDTO> getSymbolById(UUID id) {
        return Optional.of(symbolMap.get(id));
    }
}