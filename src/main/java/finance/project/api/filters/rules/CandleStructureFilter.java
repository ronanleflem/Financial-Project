package finance.project.api.filters.rules;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandleStructureFilter {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;

    public Map<String, Double> calculateContinuationProbabilities(String symbolStr, String timeframe) {
        Optional<Symbol> symbol = symbolRepository.findBySymbol(symbolStr);
        List<Candle> candles = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(symbol.get(), timeframe);

        if (candles.isEmpty()) {
            throw new IllegalStateException("Aucune donnée de bougie trouvée pour " + symbol);
        }

        int bullishCount = 0;
        int bearishCount = 0;
        int bullishToBullish = 0;
        int bearishToBearish = 0;
        int bullishToBearish = 0;
        int bearishToBullish = 0;

        for (int i = 0; i < candles.size() - 1; i++) {
            Candle current = candles.get(i);
            Candle next = candles.get(i + 1);

            boolean currentBullish = current.getClose().compareTo(current.getOpen()) > 0;
            boolean nextBullish = next.getClose().compareTo(next.getOpen()) > 0;

            if (currentBullish) {
                bullishCount++;
                if (nextBullish) {
                    bullishToBullish++;
                } else {
                    bullishToBearish++;
                }
            } else {
                bearishCount++;
                if (nextBullish) {
                    bearishToBullish++;
                } else {
                    bearishToBearish++;
                }
            }
        }

        Map<String, Double> probabilities = new HashMap<>();
        probabilities.put("Bullish → Bullish", bullishCount == 0 ? 0.0 : (bullishToBullish * 100.0 / bullishCount));
        probabilities.put("Bullish → Bearish", bullishCount == 0 ? 0.0 : (bullishToBearish * 100.0 / bullishCount));
        probabilities.put("Bearish → Bearish", bearishCount == 0 ? 0.0 : (bearishToBearish * 100.0 / bearishCount));
        probabilities.put("Bearish → Bullish", bearishCount == 0 ? 0.0 : (bearishToBullish * 100.0 / bearishCount));

        return probabilities;
    }

}
