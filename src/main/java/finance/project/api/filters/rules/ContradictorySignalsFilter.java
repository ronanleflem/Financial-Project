package finance.project.api.filters.rules;

import org.springframework.stereotype.Service;

@Service
public class ContradictorySignalsFilter {

    /**
     * Analyse les contradictions entre plusieurs indicateurs techniques.
     *
     * @param price Prix actuel
     * @param ema50 EMA 50 périodes
     * @param ema200 EMA 200 périodes
     * @param rsi Valeur RSI actuelle
     * @param macd MACD actuel
     * @param macdSignal Ligne de signal MACD
     * @param stochK Stochastique %K
     * @param stochD Stochastique %D
     * @param zScore Z-Score de la volatilité
     * @return Score de contradiction (0 = pas de conflit, 3+ = forte contradiction)
     */
    public int calculateContradictionScore(double price, double ema50, double ema200,
                                           double rsi, double macd, double macdSignal,
                                           double stochK, double stochD, double zScore) {

        int contradictionScore = 0;

        // Contradiction RSI vs Tendance
        if ((rsi > 70 && price > ema50) || (rsi < 30 && price < ema50)) {
            contradictionScore++;
        }

        // Contradiction MACD vs RSI
        if ((macd > macdSignal && rsi < 40) || (macd < macdSignal && rsi > 60)) {
            contradictionScore++;
        }

        // Contradiction Stochastique vs MACD
        if ((stochK > 80 && macd > macdSignal) || (stochK < 20 && macd < macdSignal)) {
            contradictionScore++;
        }

        // Contradiction Stochastique vs RSI
        if ((stochK > 80 && rsi < 30) || (stochK < 20 && rsi > 70)) {
            contradictionScore++;
        }

        // Z-Score élevé → Volatilité excessive, ignorer d'autres signaux
        if (Math.abs(zScore) > 2.0) {
            contradictionScore++;
        }

        return contradictionScore;
    }
}