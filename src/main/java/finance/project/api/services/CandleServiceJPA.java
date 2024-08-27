package finance.project.api.services;

import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
public class CandleServiceJPA implements CandleService {

    private final CandleRepository chartRepository;
    private final CandleMapper chartDataMapper;

    @Override
    public List<CandleDTO> getCandles(String symbol, String interval) {
        LocalDate startDate = LocalDate.now(); // To Change
        LocalDate endDate = LocalDate.now();

        return chartRepository.findBySymbolAndDateBetween(symbol, startDate, endDate)
                .stream()
                .map(chartDataMapper::toDto)
                .collect(Collectors.toList());
    }
}
