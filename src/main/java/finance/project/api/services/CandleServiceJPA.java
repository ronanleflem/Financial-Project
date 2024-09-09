package finance.project.api.services;

import finance.project.api.entities.Symbol;
import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
public class CandleServiceJPA implements CandleService {

    private final CandleRepository chartRepository;
    private final SymbolRepository symbolRepository;
    private final CandleMapper chartDataMapper;

    @Override
    public List<CandleDTO> getCandles(SymbolDTO symbol, String interval) {

        Optional<Symbol> optionalSymbol = symbolRepository.findBySymbol(symbol.getSymbol());

        if (optionalSymbol.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate startDate = LocalDate.now(); // To Change
        LocalDate endDate = LocalDate.now();

        return chartRepository.findBySymbolAndDateBetween(optionalSymbol.get(), startDate, endDate)
                .stream()
                .map(chartDataMapper::toDto)
                .collect(Collectors.toList());
    }

}
