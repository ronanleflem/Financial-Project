package finance.project.api.services;

import finance.project.api.controllers.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import finance.project.api.entities.Symbol;
import finance.project.api.mappers.SymbolMapper;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.SymbolRepository;
import org.springframework.context.annotation.Primary;

@Service
@Primary
@RequiredArgsConstructor
public class SymbolServiceJPA implements SymbolService {

    private final SymbolRepository symbolRepository;
    private final SymbolMapper symbolMapper;

    @Override
    public SymbolDTO createSymbol(SymbolDTO symbolDTO) {
        Symbol symbol = symbolMapper.toEntity(symbolDTO);
        symbol = symbolRepository.save(symbol);
        return symbolMapper.toDto(symbol);
    }

    @Override
    public SymbolDTO updateSymbol(UUID id, SymbolDTO updatedSymbolDTO) {
        return symbolRepository.findById(id)
                .map(existingSymbol -> {
                    existingSymbol.setName(updatedSymbolDTO.getName());
                    existingSymbol.setMarket(updatedSymbolDTO.getMarket());
                    return symbolMapper.toDto(symbolRepository.save(existingSymbol));
                })
                .orElseThrow(() -> new NotFoundException("Symbol not found"));
    }

    @Override
    public Boolean deleteSymbol(UUID id) {
        if (symbolRepository.existsById(id)) {
            symbolRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<SymbolDTO> listAllSymbols() {
        return symbolRepository.findAll()
                .stream()
                .map(symbolMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SymbolDTO> getSymbolById(UUID id) {
        return symbolRepository.findById(id)
                .map(symbolMapper::toDto);
    }
}