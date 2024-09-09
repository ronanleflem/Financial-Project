package finance.project.api.services;

import finance.project.api.controllers.NotFoundException;
import finance.project.api.entities.Symbol;
import finance.project.api.model.SymbolDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SymbolServiceImpl implements SymbolService {

    private final Map<UUID, SymbolDTO> symbolMap = new HashMap<>();

    public SymbolServiceImpl() {
        initializeSymbols();
    }

    private void initializeSymbols() {
        SymbolDTO symbol1 = createSymbolDTO("AAPL", "Apple Inc.", "NASDAQ");
        SymbolDTO symbol2 = createSymbolDTO("EURUSD", "Euro to US Dollar", "Forex");
        SymbolDTO symbol3 = createSymbolDTO("GOLD", "Gold Futures", "Commodities");

        symbolMap.put(symbol1.getId(), symbol1);
        symbolMap.put(symbol2.getId(), symbol2);
        symbolMap.put(symbol3.getId(), symbol3);
    }

    private SymbolDTO createSymbolDTO(String symbol, String name, String market) {
        return SymbolDTO.builder()
                .id(UUID.randomUUID())
                .symbol(symbol)
                .name(name)
                .market(market)
                .build();
    }

    @Override
    public SymbolDTO createSymbol(SymbolDTO symbolDTO) {
        UUID id = UUID.randomUUID();
        symbolDTO.setId(id);
        symbolMap.put(id, symbolDTO);
        return symbolDTO;
    }

    @Override
    public SymbolDTO updateSymbol(UUID id, SymbolDTO updatedSymbolDTO) {
        return Optional.ofNullable(symbolMap.get(id))
                .map(existingSymbol -> {
                    updatedSymbolDTO.setId(id);
                    symbolMap.put(id, updatedSymbolDTO);
                    return updatedSymbolDTO;
                })
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
    }

    @Override
    public Boolean deleteSymbol(UUID id) {
        return symbolMap.remove(id) != null;
    }

    @Override
    public List<SymbolDTO> listAllSymbols() {
        return new ArrayList<>(symbolMap.values());
    }

    @Override
    public Optional<SymbolDTO> getSymbolById(UUID id) {
        return Optional.ofNullable(symbolMap.get(id));
    }
    @Override
    public SymbolDTO getSymbolByCode(String symbolCode) {
        return Optional.ofNullable(symbolMap.get(symbolCode))
                .orElseThrow(() -> new NotFoundException("Symbol not found: " + symbolCode));
    }
}