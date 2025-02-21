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

        int bullishCount = 0, bearishCount = 0;
        int bullishToBullish = 0, bearishToBearish = 0, bullishToBearish = 0, bearishToBullish = 0;
        int bullishEngulfing = 0, bearishEngulfing = 0;
        int gapCount = 0, retraceCount = 0, openReboundCount = 0;
        int breakoutHighFirst = 0, breakoutLowFirst = 0;

        double totalBodyRatio = 0.0;
        double totalUpperWickRatio = 0.0, totalLowerWickRatio = 0.0;
        double totalCloseOpenGap = 0.0;

        int currentBullishStreak = 0, currentBearishStreak = 0;
        Map<Integer, Integer> bullishStreaks = new HashMap<>();
        Map<Integer, Integer> bearishStreaks = new HashMap<>();

        for (int i = 0; i < candles.size(); i++) {
            Candle current = candles.get(i);
            boolean isBullish = current.getClose().compareTo(current.getOpen()) > 0;
            double bodySize = Math.abs(current.getClose().doubleValue() - current.getOpen().doubleValue());
            double totalRange = current.getHigh().doubleValue() - current.getLow().doubleValue();
            double upperWick = current.getHigh().doubleValue() - Math.max(current.getClose().doubleValue(), current.getOpen().doubleValue());
            double lowerWick = Math.min(current.getClose().doubleValue(), current.getOpen().doubleValue()) - current.getLow().doubleValue();

            if (totalRange > 0) {
                totalBodyRatio += bodySize / totalRange;
                totalUpperWickRatio += upperWick / totalRange;
                totalLowerWickRatio += lowerWick / totalRange;
            }

            if (i < candles.size() - 1) {
                Candle next = candles.get(i + 1);
                boolean nextBullish = next.getClose().compareTo(next.getOpen()) > 0;

                if (isBullish) {
                    bullishCount++;
                    if (nextBullish) bullishToBullish++;
                    else bullishToBearish++;
                } else {
                    bearishCount++;
                    if (nextBullish) bearishToBullish++;
                    else bearishToBearish++;
                }

                if (isBullish && next.getClose().doubleValue() > current.getOpen().doubleValue() && next.getOpen().doubleValue() < current.getClose().doubleValue()) {
                    bullishEngulfing++;
                }
                if (!isBullish && next.getClose().doubleValue() < current.getOpen().doubleValue() && next.getOpen().doubleValue() > current.getClose().doubleValue()) {
                    bearishEngulfing++;
                }

                if (Math.abs(next.getOpen().doubleValue() - current.getClose().doubleValue()) > 0.0001) {
                    gapCount++;
                }

                if ((isBullish && current.getLow().doubleValue() < current.getOpen().doubleValue()) ||
                        (!isBullish && current.getHigh().doubleValue() > current.getOpen().doubleValue())) {
                    openReboundCount++;
                }

                if (current.getHigh().doubleValue() - current.getOpen().doubleValue() >
                        current.getOpen().doubleValue() - current.getLow().doubleValue()) {
                    breakoutHighFirst++;
                } else {
                    breakoutLowFirst++;
                }

                if (Math.abs(current.getClose().doubleValue() - current.getOpen().doubleValue()) < totalRange * 0.5) {
                    retraceCount++;
                }

                totalCloseOpenGap += Math.abs(next.getOpen().doubleValue() - current.getClose().doubleValue());
            }

            if (isBullish) {
                currentBullishStreak++;
                if (currentBearishStreak >= 2) {
                    int key = Math.min(currentBearishStreak, 3);
                    bearishStreaks.put(key, bearishStreaks.getOrDefault(key, 0) + 1);
                }
                currentBearishStreak = 0;
            } else {
                currentBearishStreak++;
                if (currentBullishStreak >= 2) {
                    int key = Math.min(currentBullishStreak, 3);
                    bullishStreaks.put(key, bullishStreaks.getOrDefault(key, 0) + 1);
                }
                currentBullishStreak = 0;
            }
        }

        if (currentBullishStreak >= 2) {
            int key = Math.min(currentBullishStreak, 3);
            bullishStreaks.put(key, bullishStreaks.getOrDefault(key, 0) + 1);
        }
        if (currentBearishStreak >= 2) {
            int key = Math.min(currentBearishStreak, 3);
            bearishStreaks.put(key, bearishStreaks.getOrDefault(key, 0) + 1);
        }

        Map<String, Double> probabilities = new HashMap<>();
        probabilities.put("Bullish → Bullish", bullishCount == 0 ? 0.0 : (bullishToBullish * 100.0 / bullishCount));
        probabilities.put("Bullish → Bearish", bullishCount == 0 ? 0.0 : (bullishToBearish * 100.0 / bullishCount));
        probabilities.put("Bearish → Bearish", bearishCount == 0 ? 0.0 : (bearishToBearish * 100.0 / bearishCount));
        probabilities.put("Bearish → Bullish", bearishCount == 0 ? 0.0 : (bearishToBullish * 100.0 / bearishCount));
        probabilities.put("Bullish Dominance", bullishCount * 100.0 / (bullishCount + bearishCount));
        probabilities.put("Bearish Dominance", bearishCount * 100.0 / (bullishCount + bearishCount));
        probabilities.put("Bullish Engulfing", bullishEngulfing * 100.0 / bullishCount);
        probabilities.put("Bearish Engulfing", bearishEngulfing * 100.0 / bearishCount);
        probabilities.put("Body Ratio", totalBodyRatio / candles.size() * 100);
        probabilities.put("Upper Wick Ratio", totalUpperWickRatio / candles.size() * 100);
        probabilities.put("Lower Wick Ratio", totalLowerWickRatio / candles.size() * 100);
        probabilities.put("Gap Probability", gapCount * 100.0 / (candles.size() - 1));
        probabilities.put("Rebound on Open", openReboundCount * 100.0 / candles.size());
        probabilities.put("Breakout High First", breakoutHighFirst * 100.0 / candles.size());
        probabilities.put("Retrace Probability", retraceCount * 100.0 / candles.size());
        probabilities.put("Avg Close-Open Gap", totalCloseOpenGap / (candles.size() - 1));
        probabilities.put("Bullish Streak 2", bullishStreaks.getOrDefault(2, 0) * 100.0 / bullishCount);
        probabilities.put("Bullish Streak 3+", bullishStreaks.getOrDefault(3, 0) * 100.0 / bullishCount);
        probabilities.put("Bearish Streak 2", bearishStreaks.getOrDefault(2, 0) * 100.0 / bearishCount);
        probabilities.put("Bearish Streak 3+", bearishStreaks.getOrDefault(3, 0) * 100.0 / bearishCount);

        return probabilities;
    }
}

