package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class MarketDataService {

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
}
