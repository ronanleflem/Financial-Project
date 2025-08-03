package finance.project.api.filters.rules;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.SignalRecorderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CandleStructureFilter implements Filter {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;

    public void recordBullishStreaks(List<Candle> candles, String symbol, String timeframe, List<Integer> horizons, SignalRecorderService recorder) {
        for (int i = 1; i < candles.size(); i++) {
            boolean bullishPrev = candles.get(i - 1).getClose().compareTo(candles.get(i - 1).getOpen()) > 0;
            boolean bullishCurr = candles.get(i).getClose().compareTo(candles.get(i).getOpen()) > 0;

            if (bullishPrev && bullishCurr) {
                double baseClose = candles.get(i).getClose().doubleValue();
                LocalDateTime time = candles.get(i).getDate();

                for (Integer h : horizons) {
                    int futureIdx = i + h;
                    if (futureIdx < candles.size()) {
                        double futureClose = candles.get(futureIdx).getClose().doubleValue();
                        recorder.recordSignal("BullishStreak2", symbol, timeframe, time, baseClose, h, futureClose);
                    }
                }
            }
        }
    }
    /**
     * Calcule l'écart-type d'une liste de valeurs.
     */
    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * Calcule le Z-Score d'une valeur par rapport à une distribution donnée.
     */
    private double calculateZScore(double value, double mean, double standardDeviation) {
        if (standardDeviation == 0) return 0.0; // Évite la division par zéro
        return (value - mean) / standardDeviation;
    }

    private void calculateTransitionProbabilities(ProbabilityData data, Candle current, Candle next) {
        boolean isBullish = current.getClose().compareTo(current.getOpen()) > 0;
        boolean nextBullish = next.getClose().compareTo(next.getOpen()) > 0;

        if (isBullish) {
            data.bullishCount++;
            if (nextBullish) data.bullishToBullish++;
            else data.bullishToBearish++;
        } else {
            data.bearishCount++;
            if (nextBullish) data.bearishToBullish++;
            else data.bearishToBearish++;
        }
    }

    private void calculateEngulfingPatterns(ProbabilityData data, Candle current, Candle next) {
        if (current.getClose().compareTo(current.getOpen()) > 0 &&
                next.getClose().doubleValue() > current.getOpen().doubleValue() &&
                next.getOpen().doubleValue() < current.getClose().doubleValue()) {
            data.bullishEngulfing++;
        }
        if (current.getClose().compareTo(current.getOpen()) < 0 &&
                next.getClose().doubleValue() < current.getOpen().doubleValue() &&
                next.getOpen().doubleValue() > current.getClose().doubleValue()) {
            data.bearishEngulfing++;
        }
    }

    private void calculateBreakoutAndRetrace(ProbabilityData data, Candle current) {
        if (current.getHigh().doubleValue() - current.getOpen().doubleValue() >
                current.getOpen().doubleValue() - current.getLow().doubleValue()) {
            data.breakoutHighFirst++;
        } else {
            data.breakoutLowFirst++;
        }

        if (Math.abs(current.getClose().doubleValue() - current.getOpen().doubleValue()) <
                (current.getHigh().doubleValue() - current.getLow().doubleValue()) * 0.5) {
            data.retraceCount++;
        }
    }

    private void calculateBodyWickRatios(ProbabilityData data, Candle current) {
        double totalRange = current.getHigh().doubleValue() - current.getLow().doubleValue();
        if (totalRange > 0) {
            double bodySize = Math.abs(current.getClose().doubleValue() - current.getOpen().doubleValue());
            double upperWick = current.getHigh().doubleValue() - Math.max(current.getClose().doubleValue(), current.getOpen().doubleValue());
            double lowerWick = Math.min(current.getClose().doubleValue(), current.getOpen().doubleValue()) - current.getLow().doubleValue();

            data.totalBodyRatio += bodySize / totalRange;
            data.totalUpperWickRatio += upperWick / totalRange;
            data.totalLowerWickRatio += lowerWick / totalRange;
        }
    }

    private void trackStreaks(ProbabilityData data, boolean isBullish) {
        if (isBullish) {
            data.currentBullishStreak++;
            if (data.currentBearishStreak >= 2) {
                data.bearishStreaks.merge(Math.min(data.currentBearishStreak, 3), 1, Integer::sum);
            }
            data.currentBearishStreak = 0;
        } else {
            data.currentBearishStreak++;
            if (data.currentBullishStreak >= 2) {
                data.bullishStreaks.merge(Math.min(data.currentBullishStreak, 3), 1, Integer::sum);
            }
            data.currentBullishStreak = 0;
        }
    }

    private void finalizeStreakCounts(ProbabilityData data) {
        if (data.currentBullishStreak >= 2) {
            data.bullishStreaks.merge(Math.min(data.currentBullishStreak, 3), 1, Integer::sum);
        }
        if (data.currentBearishStreak >= 2) {
            data.bearishStreaks.merge(Math.min(data.currentBearishStreak, 3), 1, Integer::sum);
        }
    }

    private void calculateDominance(ProbabilityData data) {
        int total = data.bullishCount + data.bearishCount;
        data.bullishDominance = total == 0 ? 0.0 : (data.bullishCount * 100.0 / total);
        data.bearishDominance = 100.0 - data.bullishDominance;
    }
    public Map<String, String> calculateContinuationProbabilities(String symbolStr, String timeframe,Integer numberLastestCandles) {
        Optional<Symbol> symbol = symbolRepository.findBySymbol(symbolStr);
        List<Candle> candles;
        if (numberLastestCandles != null && numberLastestCandles > 0) {
            candles = candleRepository.findBySymbolAndTimeframeOrderByDateAscLimitNumberLatestCandle(symbol, timeframe, numberLastestCandles);
        } else {
            candles = candleRepository.findBySymbolAndTimeframeOrderByDateAsc(symbol.get(), timeframe);
        }

        if (candles.isEmpty()) {
            throw new IllegalStateException("Aucune donnée de bougie trouvée pour " + symbolStr);
        }

        // Initialisation des compteurs
        ProbabilityData data = new ProbabilityData();
        List<Double> candleRanges = new ArrayList<>();

        for (int i = 0; i < candles.size(); i++) {
            Candle current = candles.get(i);
            boolean isBullish = current.getClose().compareTo(current.getOpen()) > 0;

            // Calcul de la taille de la bougie (high - low)
            double candleRange = current.getHigh().doubleValue() - current.getLow().doubleValue();
            candleRanges.add(candleRange);

            if (i < candles.size() - 1) {
                calculateTransitionProbabilities(data, candles.get(i), candles.get(i + 1));
                calculateEngulfingPatterns(data, candles.get(i), candles.get(i + 1));
                calculateGaps(data, candles.get(i), candles.get(i + 1));
            }

            calculateBreakoutAndRetrace(data, current);
            calculateBodyWickRatios(data, current);
            trackStreaks(data, isBullish);
        }

        finalizeStreakCounts(data);
        calculateDominance(data);
        computeAverages(data, candles.size());

        // Calcul de l'écart-type et des Z-Scores
        double standardDeviation = calculateStandardDeviation(candleRanges);
        double mean = candleRanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double lastCandleRange = candleRanges.get(candleRanges.size() - 1);
        double zScore = calculateZScore(lastCandleRange, mean, standardDeviation);

        return formatResults(data, standardDeviation, zScore);
    }

    private void calculateGaps(ProbabilityData data, Candle current, Candle next) {
        double gapSize = Math.abs(next.getOpen().doubleValue() - current.getClose().doubleValue());
        if (gapSize > 0.0001) {
            data.gapCount++;
        }
        data.totalCloseOpenGap += gapSize;
    }

    private void computeAverages(ProbabilityData data, int candleCount) {
        data.totalBodyRatio /= candleCount;
        data.totalUpperWickRatio /= candleCount;
        data.totalLowerWickRatio /= candleCount;
        data.totalCloseOpenGap /= candleCount;
    }

    private Map<String, String> formatResults(ProbabilityData data, double standardDeviation, double zScore) {
        Map<String, String> results = new HashMap<>();
        DecimalFormat df = new DecimalFormat("0.00");
        DecimalFormat dfEcartType = new DecimalFormat("0.000000");

        results.put("Bullish → Bullish", formatPercentage(data.getBullishToBullishProbability(), df));
        results.put("Bearish → Bearish", formatPercentage(data.getBearishToBearishProbability(), df));
        results.put("Bullish Engulfing", formatPercentage(data.getBullishEngulfingProbability(), df));
        results.put("Bearish Engulfing", formatPercentage(data.getBearishEngulfingProbability(), df));
        results.put("Bullish Dominance", formatPercentage(data.bullishDominance, df));
        results.put("Bearish Dominance", formatPercentage(data.bearishDominance, df));
        results.put("Bullish Streak 3+", formatPercentage(data.getBullishStreak3PlusProbability(), df));
        results.put("Bearish Streak 3+", formatPercentage(data.getBearishStreak3PlusProbability(), df));

        results.put("Gap Frequency", formatPercentage(data.getGapFrequency(), df));
        results.put("Breakout High First", formatPercentage(data.getBreakoutHighFirstProbability(), df));
        results.put("Breakout Low First", formatPercentage(data.getBreakoutLowFirstProbability(), df));
        results.put("Retracement Probability", formatPercentage(data.getRetracementProbability(), df));

        // Average values are between 0 and 1, so multiply by 100 for percentage display
        results.put("Average Body Ratio", formatPercentage(data.totalBodyRatio * 100, df));
        results.put("Average Upper Wick Ratio", formatPercentage(data.totalUpperWickRatio * 100, df));
        results.put("Average Lower Wick Ratio", formatPercentage(data.totalLowerWickRatio * 100, df));

        // Ajout des nouvelles métriques
        results.put("Standard Deviation (Écart-Type)", dfEcartType.format(standardDeviation));
        results.put("Z-Score (Anomalie Bougie)", df.format(zScore));
        return results;
    }


    private static String formatPercentage(double value, DecimalFormat df) {
        return df.format(value) + "%";
    }

    @Data
    public class ProbabilityData {
        int bullishCount = 0;
        int bearishCount = 0;
        int bullishToBullish = 0;
        int bearishToBearish = 0;
        int bullishToBearish = 0;
        int bearishToBullish = 0;

        int bullishEngulfing = 0;
        int bearishEngulfing = 0;
        int gapCount = 0;

        int breakoutHighFirst = 0;
        int breakoutLowFirst = 0;
        int retraceCount = 0;

        double totalBodyRatio = 0.0;
        double totalUpperWickRatio = 0.0;
        double totalLowerWickRatio = 0.0;
        double totalCloseOpenGap = 0.0;

        int currentBullishStreak = 0;
        int currentBearishStreak = 0;
        Map<Integer, Integer> bullishStreaks = new HashMap<>();
        Map<Integer, Integer> bearishStreaks = new HashMap<>();

        double bullishDominance = 0.0;
        double bearishDominance = 0.0;

        // Méthodes utilitaires pour éviter les divisions par zéro
        public double getBullishToBullishProbability() {
            return bullishCount == 0 ? 0.0 : (bullishToBullish * 100.0 / bullishCount);
        }

        public double getBearishToBearishProbability() {
            return bearishCount == 0 ? 0.0 : (bearishToBearish * 100.0 / bearishCount);
        }

        public double getBullishEngulfingProbability() {
            return bullishCount == 0 ? 0.0 : (bullishEngulfing * 100.0 / bullishCount);
        }

        public double getBearishEngulfingProbability() {
            return bearishCount == 0 ? 0.0 : (bearishEngulfing * 100.0 / bearishCount);
        }

        public double getBullishStreak3PlusProbability() {
            int streak3Plus = bullishStreaks.getOrDefault(3, 0) + bullishStreaks.getOrDefault(4, 0) + bullishStreaks.getOrDefault(5, 0);
            return bullishCount == 0 ? 0.0 : (streak3Plus * 100.0 / bullishCount);
        }

        public double getBearishStreak3PlusProbability() {
            int streak3Plus = bearishStreaks.getOrDefault(3, 0) + bearishStreaks.getOrDefault(4, 0) + bearishStreaks.getOrDefault(5, 0);
            return bearishCount == 0 ? 0.0 : (streak3Plus * 100.0 / bearishCount);
        }

        public double getGapFrequency() {
            int total = bullishCount + bearishCount;
            return total == 0 ? 0.0 : (gapCount * 100.0 / total);
        }

        public double getBreakoutHighFirstProbability() {
            int total = breakoutHighFirst + breakoutLowFirst;
            return total == 0 ? 0.0 : (breakoutHighFirst * 100.0 / total);
        }

        public double getBreakoutLowFirstProbability() {
            int total = breakoutHighFirst + breakoutLowFirst;
            return total == 0 ? 0.0 : (breakoutLowFirst * 100.0 / total);
        }

        public double getRetracementProbability() {
            int total = bullishCount + bearishCount;
            return total == 0 ? 0.0 : (retraceCount * 100.0 / total);
        }

    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period) {
        return 1;
    }
    
    @Override
    public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        long bullish = candles.stream().filter(c -> c.getClose().doubleValue() > c.getOpen().doubleValue()).count();
        long bearish = candles.size() - bullish;
        if (tradeRequest.getTradeSignal() == null) return 0;
        TradeSignalDTO.TradeType type = tradeRequest.getTradeSignal().getTradeType();
        boolean bullishBias = bullish >= bearish;
        if (bullishBias && type == TradeSignalDTO.TradeType.LONG) return 1;
        if (!bullishBias && type == TradeSignalDTO.TradeType.SHORT) return 1;
        return 0;
    }
}
