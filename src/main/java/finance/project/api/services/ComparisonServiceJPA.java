package finance.project.api.services;

import finance.project.api.controllers.NotFoundException;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Comparison;
import finance.project.api.entities.Symbol;
import finance.project.api.model.PerformanceComparisonDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.ComparisonRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class ComparisonServiceJPA implements ComparisonService {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final ComparisonRepository comparisonRepository;

    public PerformanceComparisonDTO comparePerformance(String symbol1, String symbol2, LocalDate startDate, LocalDate endDate) {
        Optional<Symbol> optionalSymbol1 = symbolRepository.findBySymbol(symbol1);
        Optional<Symbol> optionalSymbol2 = symbolRepository.findBySymbol(symbol2);

        if (optionalSymbol1.isEmpty() || optionalSymbol2.isEmpty()) {
            throw new NotFoundException("Un ou les deux symboles ne sont pas trouvés.");
        }

        List<Candle> symbol1Candles = candleRepository.findBySymbolAndDateBetween(optionalSymbol1.get(), startDate, endDate);
        List<Candle> symbol2Candles = candleRepository.findBySymbolAndDateBetween(optionalSymbol2.get(), startDate, endDate);

        BigDecimal performance1 = calculatePerformance(symbol1Candles);
        BigDecimal performance2 = calculatePerformance(symbol2Candles);

        Symbol symbolEntity1 = symbol1Candles.get(0).getSymbol();
        Symbol symbolEntity2 = symbol2Candles.get(0).getSymbol();

        Comparison comparison = new Comparison(
                UUID.randomUUID(),
                symbolEntity1,
                symbolEntity2,
                performance1,
                performance2,
                startDate,
                endDate
        );
        comparisonRepository.save(comparison);

        return new PerformanceComparisonDTO(symbol1, performance1, symbol2, performance2);
    }

    private BigDecimal calculatePerformance(List<Candle> candles) {
        if (candles.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal startPrice = candles.get(0).getClose();
        BigDecimal endPrice = candles.get(candles.size() - 1).getClose();
        return (endPrice.subtract(startPrice)).divide(startPrice, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}