package finance.project.api.services;

import finance.project.api.model.SymbolDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SymbolService {

    SymbolDTO createSymbol(SymbolDTO symbolDTO);

    SymbolDTO updateSymbol(UUID id, SymbolDTO updatedSymbolDTO);

    Boolean deleteSymbol(UUID id);

    List<SymbolDTO> listAllSymbols();

    Optional<SymbolDTO> getSymbolById(UUID id);
}