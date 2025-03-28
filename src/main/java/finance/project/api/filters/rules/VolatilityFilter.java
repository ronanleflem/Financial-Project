package finance.project.api.filters.rules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtre basé sur la volatilité :
 * - Z-score : Analyse statistique de la volatilité pour détecter des extrêmes
 * - Entropy : Mesure du chaos dans le marché (range vs. tendance)
 * - Volatility : ATR, Bollinger Bands pour évaluer la volatilité actuelle
 */
@Service
public class VolatilityFilter {

    private final BarSeries series = null;
    private Indicator<Num> closePrice = null;
    private final int atrPeriod = 14;
    private final int bollingerPeriod = 20;
    private final double bollingerMultiplier = 2.0;

    /**
     * Calcule l'entropie de Shannon des variations de prix.
     *
     * @param priceChanges Liste des variations de prix successives
     * @return Valeur d'entropie (0 = marché structuré, proche de 1 = marché chaotique)
     */
    public double calculateMarketEntropy(List<Double> priceChanges) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        for (double change : priceChanges) {
            int bucket = (int) Math.round(change * 1000); // Regroupement des valeurs
            frequencyMap.put(bucket, frequencyMap.getOrDefault(bucket, 0) + 1);
        }

        double entropy = 0.0;
        int totalCount = priceChanges.size();

        for (int count : frequencyMap.values()) {
            double probability = (double) count / totalCount;
            entropy -= probability * Math.log(probability) / Math.log(2);
        }

        return entropy / Math.log(totalCount);
    }

    public double calculateVIXApproximation() {
        ATRIndicator atr = new ATRIndicator(series, atrPeriod);
        double atrValue = atr.getValue(series.getEndIndex()).doubleValue();

        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, bollingerPeriod);
        double stdDevValue = stdDev.getValue(series.getEndIndex()).doubleValue();

        return (atrValue / stdDevValue) * 100; // Normalisation arbitraire
    }


    public Map<String, Double> analyzeVolatility(BarSeries series) {
        Map<String, Double> volatilityData = new HashMap<>();

        this.closePrice = new ClosePriceIndicator(series);

        // ATR Calculation
        ATRIndicator atr = new ATRIndicator(series, atrPeriod);
        double atrValue = atr.getValue(series.getEndIndex()).doubleValue();

        SMAIndicator sma = new SMAIndicator(closePrice, bollingerPeriod);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, bollingerPeriod);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, standardDeviation);
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, standardDeviation);

        double upperValue = upperBand.getValue(series.getEndIndex()).doubleValue();
        double lowerValue = lowerBand.getValue(series.getEndIndex()).doubleValue();

        // Store results
        volatilityData.put("ATR", atrValue);
        volatilityData.put("BollingerUpper", upperValue);
        volatilityData.put("BollingerLower", lowerValue);
        volatilityData.put("VIX_Approximation", calculateVIXApproximation());

        return volatilityData;
    }
}
