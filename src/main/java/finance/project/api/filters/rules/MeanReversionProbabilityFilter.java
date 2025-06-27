package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.services.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Probabilité de retour à la moyenne (permet d'exclure des tendances trop étirées).
 */
@Service
public class MeanReversionProbabilityFilter implements Filter {
    private final MarketDataService marketDataService;

    @Autowired
    public MeanReversionProbabilityFilter(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public boolean isMeanReversionSignal(List<CandleDTO> candles, int period, String timeframe) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Pas assez de données pour calculer les indicateurs.");
        }

        // Calcul des Bandes de Bollinger
        double sma = marketDataService.calculateSMA(candles, period);
        double stdDev = marketDataService.calculateStandardDeviation(candles, sma);
        double upperBollingerBand = sma + (2 * stdDev);
        double lowerBollingerBand = sma - (2 * stdDev);

        // Calcul des Canaux de Keltner
        double ema = marketDataService.calculateEMA(candles, period);
        double atr = marketDataService.calculateATR(candles, period, timeframe);
        double upperKeltnerChannel = ema + (2 * atr);
        double lowerKeltnerChannel = ema - (2 * atr);

        // Dernière bougie pour l'analyse
        CandleDTO lastCandle = candles.get(candles.size() - 1);
        double closePrice = lastCandle.getClose().doubleValue();

        // Détection des signaux de retournement
        boolean isOverbought = closePrice > upperBollingerBand && closePrice > upperKeltnerChannel;
        boolean isOversold = closePrice < lowerBollingerBand && closePrice < lowerKeltnerChannel;

        // Logique de décision
        if (isOverbought) {
            // Surachat détecté
            return false; // Exclure le trade
        } else if (isOversold) {
            // Survente détectée
            return true; // Signal de retournement potentiel
        }

        // Aucune condition de retournement détectée
        return false;
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
        boolean signal = isMeanReversionSignal(candles, 20, "M1");
        return signal ? 1 : 0;
    }
}
