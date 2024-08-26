package finance.project.api.services;

import finance.project.api.mappers.ChartDataMapper;
import finance.project.api.model.ChartDataDTO;
import finance.project.api.repositories.ChartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Primary
@RequiredArgsConstructor
public class ChartServiceJPA implements ChartService {

    private final ChartRepository chartRepository;
    private final ChartDataMapper chartDataMapper;

    @Override
    public List<ChartDataDTO> getChartData(String symbol, String interval) {
        LocalDate startDate = LocalDate.now(); // To Change
        LocalDate endDate = LocalDate.now();

        return chartRepository.findBySymbolAndDateBetween(symbol, startDate, endDate)
                .stream()
                .map(chartDataMapper::toDto)
                .collect(Collectors.toList());
    }
}
