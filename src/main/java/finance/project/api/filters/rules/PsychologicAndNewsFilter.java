package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtrage des Événements & Psychologie du Marché
 *
 *     NewsImpactFilter.java → Éviter les annonces économiques à fort impact.
 *     TraderPsychologyFilter.java → Influence des comportements de foule.
 */
@Service
public class PsychologicAndNewsFilter implements Filter {

    public double calculatePercentageDrawdown(double currentClose, double highestClose) {
        return ((currentClose - highestClose) / highestClose) * 100;
    }

    public double calculateUlcerIndex(List<Double> closes, int period) {
        double highestClose = Double.MIN_VALUE;
        List<Double> drawdowns = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            highestClose = Math.max(highestClose, closes.get(i));
            double drawdown = calculatePercentageDrawdown(closes.get(i), highestClose);
            drawdowns.add(drawdown);
            if (drawdowns.size() > period) {
                drawdowns.remove(0);
            }
            if (drawdowns.size() == period) {
                double squaredSum = 0.0;
                for (double dd : drawdowns) {
                    squaredSum += dd * dd;
                }
                double squaredAverage = squaredSum / period;
                return Math.sqrt(squaredAverage);
            }
        }
        return 0.0;
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
        List<Double> closes = candles.stream().map(c -> c.getClose().doubleValue()).toList();
        double ui = calculateUlcerIndex(closes, 14);
        return ui < 5 ? 1 : 0;
    }
}
