package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final double RISK_FREE_RATE = 0.02; // Taux sans risque (par exemple 2%)
    private final TA4JService ta4JService;

    @Autowired
    public MarketDataService(TA4JService ta4JService) {
        this.ta4JService = ta4JService;
    }

    /**
     * Calcule le VWAP (Volume Weighted Average Price)
     *
     * @return VWAP
     */
    public double calculateVWAP(List<CandleDTO> candlesLatest) {
        if (candlesLatest.size() != candlesLatest.size() || candlesLatest.isEmpty() || candlesLatest.getFirst().getVolume() == null) {
            throw new IllegalArgumentException("Données invalides pour calculer le VWAP");
        }

        double cumulativePriceVolume = 0.0;
        double cumulativeVolume = 0.0;

        for (int i = 0; i < candlesLatest.size(); i++) {
            cumulativePriceVolume += candlesLatest.get(i).getClose().doubleValue() * candlesLatest.get(i).getVolume().doubleValue();
            cumulativeVolume += candlesLatest.get(i).getVolume().doubleValue();
        }

        return cumulativeVolume > 0 ? cumulativePriceVolume / cumulativeVolume : 0;
    }
    /**
     * Calcule une EMA (Exponential Moving Average)
     *
     * @param period Période de l'EMA
     * @return Valeur EMA
     */
    public double calculateEMA(List<CandleDTO> candleLatest, int period) {
        if (candleLatest.size() < period) {
            throw new IllegalArgumentException("Pas assez de données pour calculer l'EMA");
        }

        double multiplier = 2.0 / (period + 1);
        double ema = candleLatest.get(0).getClose().doubleValue(); // Initialisation avec le premier prix

        for (int i = 1; i < candleLatest.size(); i++) {
            ema = ((candleLatest.get(i).getClose().doubleValue() - ema) * multiplier) + ema;
        }
        return ema;
    }

    /**
     * Calcule le RSI (Relative Strength Index).
     */
    public double calculateRSI(List<CandleDTO> candleLatest, int period) {
        if (candleLatest.size() < period + 1) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le RSI");
        }

        double gainSum = 0, lossSum = 0;

        for (int i = 1; i <= period; i++) {
            double change = candleLatest.get(i).getClose().doubleValue() - candleLatest.get(i - 1).getClose().doubleValue();
            if (change > 0) gainSum += change;
            else lossSum += Math.abs(change);
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;

        return 100 - (100 / (1 + rs));
    }

    /**
     * Calcule la valeur MACD.
     */
    public double calculateMACD(List<CandleDTO> candleLatest, int shortPeriod, int longPeriod) {
        if (candleLatest.size() < longPeriod) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le MACD");
        }

        double emaShort = calculateEMA(candleLatest, shortPeriod);
        double emaLong = calculateEMA(candleLatest, longPeriod);

        return emaShort - emaLong;
    }

    /**
     * Calcule la ligne de signal MACD.
     */
    public double calculateMACDSignal(List<CandleDTO> candleLatest, int shortPeriod, int longPeriod, int signalPeriod) {
        if (candleLatest.size() < longPeriod + signalPeriod) {
            throw new IllegalArgumentException("Pas assez de données pour calculer la ligne de signal MACD");
        }

        double macd = calculateMACD(candleLatest, shortPeriod, longPeriod);
        return calculateEMA(candleLatest.subList(candleLatest.size() - signalPeriod, candleLatest.size()), signalPeriod);
    }

    /**
     * Calcule le Stochastique %K.
     */
    public double calculateStochasticK(List<CandleDTO> candleLatest, int period) {
        if (candleLatest.size() < period) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le Stochastique %K");
        }

        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        double currentClose = candleLatest.get(candleLatest.size() - 1).getClose().doubleValue();

        for (int i = candleLatest.size() - period; i < candleLatest.size(); i++) {
            highestHigh = Math.max(highestHigh, candleLatest.get(i).getHigh().doubleValue());
            lowestLow = Math.min(lowestLow, candleLatest.get(i).getLow().doubleValue());
        }

        return ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100;
    }

    /**
     * Calcule le Stochastique %D (Moyenne mobile de %K).
     */
    public double calculateStochasticD(List<CandleDTO> candleLatest, int period, int smoothing) {
        if (candleLatest.size() < period + smoothing) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le Stochastique %D");
        }

        double sumK = 0.0;

        for (int i = candleLatest.size() - smoothing; i < candleLatest.size(); i++) {
            sumK += calculateStochasticK(candleLatest.subList(0, i), period);
        }

        return sumK / smoothing;
    }
    public double calculateWilliamsR(List<CandleDTO> candles, int period) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Pas assez de bougies pour calculer Williams %R.");
        }

        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        double closePrice = candles.get(candles.size() - 1).getClose().doubleValue();

        for (int i = candles.size() - period; i < candles.size(); i++) {
            highestHigh = Math.max(highestHigh, candles.get(i).getHigh().doubleValue());
            lowestLow = Math.min(lowestLow, candles.get(i).getLow().doubleValue());
        }

        return ((highestHigh - closePrice) / (highestHigh - lowestLow)) * -100;
    }

    /**
     * Calcule le Z-Score de la volatilité des bougies.
     */
    public double calculateZScore(List<CandleDTO> candleLatest, int period) {
        if (candleLatest.size() < period) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le Z-Score");
        }

        double mean = candleLatest.stream().mapToDouble(c -> c.getClose().doubleValue()).average().orElse(0.0);

        double variance = candleLatest.stream().mapToDouble(c ->
                Math.pow(c.getClose().doubleValue() - mean, 2)).average().orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double lastClose = candleLatest.get(candleLatest.size() - 1).getClose().doubleValue();

        return (lastClose - mean) / stdDev;
    }

    public double calculatePOC(List<CandleDTO> candles) {
        Map<Double, Double> volumeProfile = calculateVolumeProfile( candles, 50);
        return volumeProfile.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalArgumentException("No data in volume profile"));
    }

    public double calculateVAH(List<CandleDTO> candles) {
        Map<Double, Double> volumeProfile = calculateVolumeProfile( candles, 50);
        double totalVolume = volumeProfile.values().stream().mapToDouble(Double::doubleValue).sum();
        double volumeThreshold = totalVolume * 0.7;

        List<Map.Entry<Double, Double>> sortedEntries = volumeProfile.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        double cumulativeVolume = 0.0;
        double vah = 0.0;
        for (Map.Entry<Double, Double> entry : sortedEntries) {
            cumulativeVolume += entry.getValue();
            if (cumulativeVolume >= volumeThreshold) {
                vah = entry.getKey();
                break;
            }
        }
        return vah;
    }

    public double calculateVAL(List<CandleDTO> candles) {
        Map<Double, Double> volumeProfile = calculateVolumeProfile( candles, 50); // avec 50 niveaux de prix
        double totalVolume = volumeProfile.values().stream().mapToDouble(Double::doubleValue).sum();
        double volumeThreshold = totalVolume * 0.3;

        List<Map.Entry<Double, Double>> sortedEntries = volumeProfile.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        double cumulativeVolume = 0.0;
        double val = 0.0;
        for (int i = sortedEntries.size() - 1; i >= 0; i--) {
            cumulativeVolume += sortedEntries.get(i).getValue();
            if (cumulativeVolume >= volumeThreshold) {
                val = sortedEntries.get(i).getKey();
                break;
            }
        }
        return val;
    }

    public Map<Double, Double> calculateVolumeProfile(List<CandleDTO> candles, int binCount) {
        if (candles.isEmpty()) {
            throw new IllegalArgumentException("La liste des bougies est vide.");
        }

        double highestPrice = candles.stream().mapToDouble(c -> c.getHigh().doubleValue()).max().orElseThrow();
        double lowestPrice = candles.stream().mapToDouble(c -> c.getLow().doubleValue()).min().orElseThrow();

        // Déterminer l'intervalle de prix par bin
        double binSize = (highestPrice - lowestPrice) / binCount;
        Map<Double, Double> volumeProfile = new TreeMap<>();

        // Initialiser le volume par niveau de prix
        for (int i = 0; i < binCount; i++) {
            double priceLevel = lowestPrice + (i * binSize);
            volumeProfile.put(priceLevel, 0.0);
        }

        // Ajouter les volumes aux niveaux de prix correspondants
        for (CandleDTO candle : candles) {
            double price = (candle.getHigh().doubleValue() + candle.getLow().doubleValue()) / 2.0; // Niveau médian
            double volume = candle.getVolume().doubleValue();

            // Trouver le bin correspondant
            double closestLevel = volumeProfile.keySet().stream()
                    .min(Comparator.comparingDouble(level -> Math.abs(level - price)))
                    .orElseThrow();

            // Ajouter le volume au niveau correspondant
            volumeProfile.put(closestLevel, volumeProfile.get(closestLevel) + volume);
        }

        return volumeProfile;
    }
    public double calculateSMA(List<CandleDTO> candles, int period) {
        double sum = 0;
        for (int i = candles.size() - period; i < candles.size(); i++) {
            sum += candles.get(i).getClose().doubleValue();
        }
        return sum / period;
    }

    public double calculateSharpeRatio(List<CandleDTO> returns) {
        double meanReturn = calculateMean(returns);
        double stdDev = calculateStandardDeviation(returns, meanReturn);
        return (meanReturn - RISK_FREE_RATE) / stdDev;
    }

    public double calculateSortinoRatio(List<CandleDTO> returns) {
        double meanReturn = calculateMean(returns);
        double downsideStdDev = calculateDownsideStandardDeviation(returns, meanReturn);
        return (meanReturn - RISK_FREE_RATE) / downsideStdDev;
    }

    private double calculateMean(List<CandleDTO> returns) {
        List<Double> closePrices = returns.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());
        return closePrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double calculateStandardDeviation(List<CandleDTO> returns, double mean) {
        List<Double> closePrices = returns.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());
        return Math.sqrt(closePrices.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0));
    }

    private double calculateStandardDeviationDouble(List<Double> returns, double mean) {
        return Math.sqrt(returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0));
    }

    private double calculateDownsideStandardDeviation(List<CandleDTO> returns, double mean) {
        List<Double> closePrices = returns.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());
        List<Double> downsideReturns = closePrices.stream()
                .filter(r -> r < mean)  // Ne prendre que les retours inférieurs à la moyenne
                .toList();

        return calculateStandardDeviationDouble(downsideReturns, mean);
    }

    public double calculateATR(List<CandleDTO> candles, int period, String timeframe) {

        BarSeries series = ta4JService.convertToTimeSeries(candles,timeframe);
        // ATR Calculation
        ATRIndicator atr = new ATRIndicator(series, period);
        return atr.getValue(series.getEndIndex()).doubleValue();
    }
}
