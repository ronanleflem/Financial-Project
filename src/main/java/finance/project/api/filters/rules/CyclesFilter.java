package finance.project.api.filters.rules;


import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CyclesFilter implements Filter {

    /**
     * Détecte la période dominante d'un cycle en analysant la répétition des tendances.
     *
     * @param priceChanges Liste des variations de prix successives
     * @return Période dominante détectée (en nombre de bougies)
     */
    public int detectDominantCycle(List<Double> priceChanges) {
        int maxLag = Math.min(50, priceChanges.size() / 2); // On limite l’analyse aux 50 dernières bougies max
        int bestPeriod = 1;
        double bestCorrelation = 0;

        for (int lag = 2; lag <= maxLag; lag++) {
            double correlation = calculateAutoCorrelation(priceChanges, lag);
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation;
                bestPeriod = lag;
            }
        }

        return bestPeriod;
    }

    /**
     * Calcule l'autocorrélation pour une période donnée.
     */
    private double calculateAutoCorrelation(List<Double> prices, int lag) {
        if (prices.size() < lag * 2) return 0.0;

        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double numerator = 0, denominator = 0;

        for (int i = 0; i < prices.size() - lag; i++) {
            numerator += (prices.get(i) - mean) * (prices.get(i + lag) - mean);
            denominator += Math.pow(prices.get(i) - mean, 2);
        }

        return (denominator == 0) ? 0 : numerator / denominator;
    }

    /**
     * Calcule le R² pour déterminer si le marché suit une tendance stable.
     *
     * @param prices Liste des prix successifs
     * @return R² (proche de 1 = tendance forte, proche de 0 = marché erratique)
     */
    public double calculateR2(List<Double> prices) {
        if (prices.size() < 20) return 0.0; // Pas assez de données pour une bonne régression

        int n = prices.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += prices.get(i);
            sumXY += i * prices.get(i);
            sumX2 += i * i;
            sumY2 += prices.get(i) * prices.get(i);
        }

        double numerator = (n * sumXY - sumX * sumY) * (n * sumXY - sumX * sumY);
        double denominator = (n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY);

        return denominator == 0 ? 0 : numerator / denominator;
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
        if (candles.size() < 30) return 0;
        List<Double> closes = candles.stream().map(c -> c.getClose().doubleValue()).toList();
        double r2 = calculateR2(closes);
        return r2 < 0.5 ? 1 : 0;
    }
}
