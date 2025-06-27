package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
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
public class VolatilityFilter implements Filter {

    private Indicator<Num> closePrice = null;
    private final int atrPeriod = 14;
    private final int bollingerPeriod = 20;

    /**
     * Calcule l'entropie de Shannon des variations de prix.
     * Tu peux l'ajuster selon le contexte :
     * Pour l’EUR/USD en pips → scaleFactor = 10000 (car 1 pip = 0.0001)
     * Pour un indice comme le Nasdaq → scaleFactor = 1 ou 10 (car il bouge en points entiers)
     * Si tu veux lisser davantage → scaleFactor = 1000 pour regrouper par 0.1 pip
     * @param priceChanges Liste des variations de prix successives
     * @return Valeur d'entropie (0 = marché structuré, proche de 1 = marché chaotique)
     */
    public double calculateMarketEntropy(List<Double> priceChanges, int scaleFactor) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        for (double change : priceChanges) {
            int bucket = (int) Math.round(change * scaleFactor); // Regroupement des valeurs
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

    /**
     * A utiliser dans la grosse fonction sinon faut initialiser closePrice
     * APPROXIMATION FAUSSE
     * @param series
     * @return
     */
    public double calculateVIXApproximation(BarSeries series) {
        ATRIndicator atr = new ATRIndicator(series, atrPeriod);

        double atrValue = atr.getValue(series.getEndIndex()).doubleValue();

        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, bollingerPeriod);
        double stdDevValue = stdDev.getValue(series.getEndIndex()).doubleValue();

        return stdDevValue == 0 ? 0 : (atrValue / stdDevValue) * 100;// Normalisation arbitraire
    }

    public double calculateHistoricalVolatility(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, period);
        Num stdDevValue = standardDeviation.getValue(series.getEndIndex());
        return stdDevValue.doubleValue();
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
        volatilityData.put("VIX_Approximation", calculateVIXApproximation(series));
        volatilityData.put("Historical volatility : ", calculateHistoricalVolatility(series, 50));

        return volatilityData;
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
        if (candles.size() < 20) return 0;
        List<Double> changes = new java.util.ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            changes.add(candles.get(i).getClose().doubleValue() - candles.get(i-1).getClose().doubleValue());
        }
        double entropy = calculateMarketEntropy(changes, 10000);
        return entropy < 0.7 ? 1 : 0;
    }
}
