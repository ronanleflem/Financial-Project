package finance.project.api.services;

import finance.project.api.model.SymbolDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SymbolServiceImpl implements SymbolService {

    private final Map<UUID, SymbolDTO> symbolMap = new HashMap<>();

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