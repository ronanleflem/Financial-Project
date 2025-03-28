package finance.project.api.filters.rules;

import finance.project.api.entities.MarketData;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import org.springframework.stereotype.Component;

/**
 *
 * Trend and Market regime filter : détection du régime de marché
 * TrendFilter.java → Détection de la tendance générale (HH/HL - LH/LL, EMA, ADX).
 * MarketRegimeFilter.java → Identification du type de marché (tendance, range, compression).
 */
@Component
public class TrendFilter implements Filter {

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, MarketData marketData) {
        double ema50 = 1;//marketData.getEma(50);
        double ema200 = 2;//marketData.getEma(200);

        if (ema50 > ema200) {
            return 10;  // Bonus si on suit la tendance haussière
        } else if (ema50 < ema200) {
            return -10; // Pénalité si on trade contre la tendance
        }
        return 0; // Neutre
    }
}