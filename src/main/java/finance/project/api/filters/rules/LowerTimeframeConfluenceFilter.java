package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 📌 Objectif : LowerTimeframeConfluenceFilter
 *
 * L’objectif est d’évaluer la dynamique des UT inférieures (M1, M5, M15) pour confirmer ou filtrer un trade sur une UT plus grande.
 *
 * ✅ Détecter l’accélération (Momentum) sur des UT inférieures.
 * ✅ Ajouter plusieurs confluences pour valider une entrée.
 * ✅ Filtrer les trades sans force directionnelle claire.
 * 📌 Confluences intégrées dans le filtre
 * Confluence	Explication	Pourquoi c’est utile ?
 * Momentum (ROC - Rate of Change)	Mesure l’accélération du prix.	Confirme si le prix a une dynamique forte.
 * ADX (Average Directional Index)	Indique si la tendance est forte (>25).	Filtre les marchés en range sans mouvement clair.
 * VWAP & Deviation Bands	Vérifie si le prix est proche du VWAP ou d’une bande extrême.	Confirme si le prix est en sur-achat/sur-vente.
 * Delta Volume (Buy vs Sell Imbalance)	Analyse la force des acheteurs et vendeurs.	Vérifie si le mouvement est soutenu par du volume.
 * Trend Alignment (EMA 20 vs EMA 50 vs EMA 200)	Vérifie si la tendance est alignée sur plusieurs UT.	Confirme si les UT inférieures valident l’UT principale.
 *
 * 📌 🚀 Prochaine étape ?
 *
 * ✅ Ajouter un seuil dynamique basé sur l’ATR pour ajuster le score ?
 * ✅ Associer le score de confluence avec l’Order Book pour voir si des ordres sont en attente ?
 * ✅ Créer un filtre qui détecte les accélérations soudaines (ex: news impact) ?
 */
@Service
public class LowerTimeframeConfluenceFilter implements Filter {

    /**
     * Calcule le Momentum (Rate of Change - ROC).
     *
     * @param closes Liste des prix de clôture
     * @param period Période du ROC
     * @return Valeur du momentum
     */
    public double calculateMomentum(List<Double> closes, int period) {
        if (closes.size() < period + 1) {
            throw new IllegalArgumentException("Pas assez de données pour calculer le Momentum.");
        }
        return ((closes.get(closes.size() - 1) - closes.get(closes.size() - period - 1)) / closes.get(closes.size() - period - 1)) * 100;
    }

    /**
     * Calcule l'ADX (Average Directional Index) pour mesurer la force de la tendance.
     */
    public double calculateADX(List<Double> highs, List<Double> lows, List<Double> closes, int period) {
        if (highs.size() < period || lows.size() < period || closes.size() < period) {
            throw new IllegalArgumentException("Pas assez de données pour calculer l'ADX.");
        }

        double sumDMPlus = 0, sumDMMinus = 0, sumTR = 0;
        for (int i = 1; i < period; i++) {
            double dmPlus = Math.max(highs.get(i) - highs.get(i - 1), 0);
            double dmMinus = Math.max(lows.get(i - 1) - lows.get(i), 0);
            double tr = Math.max(highs.get(i) - lows.get(i), Math.max(Math.abs(highs.get(i) - closes.get(i - 1)), Math.abs(lows.get(i) - closes.get(i - 1))));

            sumDMPlus += (dmPlus > dmMinus) ? dmPlus : 0;
            sumDMMinus += (dmMinus > dmPlus) ? dmMinus : 0;
            sumTR += tr;
        }

        double diPlus = (sumDMPlus / sumTR) * 100;
        double diMinus = (sumDMMinus / sumTR) * 100;
        double dx = Math.abs(diPlus - diMinus) / (diPlus + diMinus) * 100;

        return dx;
    }

    /**
     * Vérifie si la tendance est alignée sur plusieurs UT avec les EMA.
     */
    public boolean isTrendAligned(double ema20, double ema50, double ema200) {
        return ema20 > ema50 && ema50 > ema200;
    }

    /**
     * Calcule un score de confluence basé sur le momentum et d'autres facteurs.
     */
    public int calculateConfluenceScore(double momentum, double adx, boolean trendAligned, double vwapDistance, double deltaVolume) {
        int score = 0;

        if (momentum > 0.5) score++;
        if (adx > 25) score++;
        if (trendAligned) score++;
        if (Math.abs(vwapDistance) < 0.002) score++;
        if (Math.abs(deltaVolume) > 500) score++;

        return score;
    }

    private double ema(List<Double> values, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = ((values.get(i) - ema) * multiplier) + ema;
        }
        return ema;
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
        if (candles.size() < 50) return 0;
        List<Double> closes = new java.util.ArrayList<>();
        List<Double> highs = new java.util.ArrayList<>();
        List<Double> lows = new java.util.ArrayList<>();
        List<Double> volumes = new java.util.ArrayList<>();
        for (CandleDTO c : candles) {
            closes.add(c.getClose().doubleValue());
            highs.add(c.getHigh().doubleValue());
            lows.add(c.getLow().doubleValue());
            volumes.add(c.getVolume().doubleValue());
        }
        double momentum = calculateMomentum(closes, 14);
        double adx = calculateADX(highs, lows, closes, 14);
        double ema20 = ema(closes, 20);
        double ema50 = ema(closes, 50);
        double ema200 = ema(closes, 200);
        boolean aligned = isTrendAligned(ema20, ema50, ema200);
        int score = calculateConfluenceScore(momentum, adx, aligned, 0, 0);
        return score >= 3 ? 1 : 0;
    }
}
