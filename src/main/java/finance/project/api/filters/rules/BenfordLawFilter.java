package finance.project.api.filters.rules;

import finance.project.api.entities.Symbol;
import finance.project.api.filters.Filter;
import finance.project.api.model.TradeRequestDTO;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.services.CandleService;
import finance.project.api.services.SignalRecorderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📌 Quelle granularité utiliser ?
 *
 *     M1 (1 minute) → Bonne précision pour détecter des anomalies courtes (idéal pour scalping).
 *     M5 / M15 → Meilleur pour détecter des manipulations sur plusieurs heures.
 *     H1 et plus → Analyse macro, utile pour voir si le marché est sain sur plusieurs jours.
 *
 * 📌 Approche hybride :
 *
 *     Vérifier sur M1 (1000 bougies) et M15 (500 bougies) pour voir si l’anomalie est localisée.
 *     Si les deux confirment, alors forte probabilité de marché non naturel.
 *
 * 💡 Possibilités d'amélioration
 *
 *     Appliquer la loi de Benford sur d'autres metrics
 *         Taille des mèches (high - low)
 *         Volume
 *         ATR (volatilité récente)
 */
@Service
public class BenfordLawFilter implements Filter {

    private final CandleService candleService;

    // Distribution théorique des premiers chiffres selon la loi de Benford
    private static final double[] BENFORD_DISTRIBUTION = {
            0.0, 0.301, 0.176, 0.125, 0.097, 0.079, 0.067, 0.058, 0.051, 0.046
    };

    private static final double THRESHOLD = 0.02; // Seuil d'anomalie acceptable

    @Autowired
    public BenfordLawFilter(CandleService candleService) {
        this.candleService = candleService;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest) {
        List<Double> priceChanges = candleService.getPriceVariations("EURUSD", "1min", tradeRequest.getTimestamp(),tradeRequest.getTimestamp().minusMinutes(1000)); // Supposons que la DTO contient ces données
        double score = calculateBenfordScore(priceChanges);

        return (score > THRESHOLD) ? 0 : 1; // 1 = conforme
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, Symbol symbol, String timeframe, int period) {
        return 1;
    }

    @Override
    public int evaluate(TradeRequestDTO tradeRequest, String symbol, String timeframe, int period) {
        return 1;
    }

    public double getThreshold() {
        return THRESHOLD;
    }
    public void recordBenfordAnomaliesSliced(List<CandleDTO> candles,
                                             String symbol,
                                             String timeframe,
                                             int windowSize,
                                             List<Integer> horizons,
                                             SignalRecorderService recorder) {

        List<Double> priceChanges = extractPriceChanges(candles);
        List<List<Double>> slices = splitIntoSubwindows(priceChanges, windowSize);

        for (int i = 0; i < slices.size(); i++) {
            List<Double> slice = slices.get(i);
            double score = calculateBenfordScore(slice);

            if (score > getThreshold()) {
                int baseIdx = i * windowSize + windowSize - 1;
                if (baseIdx >= candles.size()) continue;
                CandleDTO base = candles.get(baseIdx);
                double baseClose = base.getClose().doubleValue();
                LocalDateTime time = base.getDate();

                for (Integer h : horizons) {
                    int targetIdx = baseIdx + h;
                    if (targetIdx < candles.size()) {
                        double futureClose = candles.get(targetIdx).getClose().doubleValue();
                        recorder.recordSignal("BenfordAnomaly", symbol, timeframe, time, baseClose, h, futureClose);
                    }
                }
            }
        }
    }
    public List<Double> extractPriceChanges(List<CandleDTO> candles) {
        List<Double> changes = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            double diff = Math.abs(candles.get(i).getClose().doubleValue() - candles.get(i - 1).getClose().doubleValue());
            changes.add(diff);
        }
        return changes;
    }

    public void recordBenfordAnomalies(List<Double> changes, List<CandleDTO> candles, String symbol, String timeframe, List<Integer> horizons, SignalRecorderService recorder) {
        double score = calculateBenfordScore(changes);
        if (score > THRESHOLD) {
            int lastIdx = changes.size() - 1;
            CandleDTO base = candles.get(lastIdx);
            double baseClose = base.getClose().doubleValue();
            LocalDateTime time = base.getDate();

            for (Integer h : horizons) {
                int targetIdx = lastIdx + h;
                if (targetIdx < candles.size()) {
                    double futureClose = candles.get(targetIdx).getClose().doubleValue();
                    recorder.recordSignal("BenfordAnomaly", symbol, timeframe, time, baseClose, h, futureClose);
                }
            }
        }
    }

    public List<List<Double>> splitIntoSubwindows(List<Double> data, int windowSize) {
        List<List<Double>> slices = new ArrayList<>();
        for (int i = 0; i <= data.size() - windowSize; i += windowSize) {
            List<Double> window = data.subList(i, i + windowSize);
            slices.add(new ArrayList<>(window));
        }
        return slices;
    }

    /**
     * Calcule le score de conformité à la loi de Benford sur une liste de variations de prix.
     *
     * @param priceChanges Liste des variations de prix.
     * @return Score d'écart (0 = parfaitement conforme, > 0 = anomalie détectée).
     */
    public double calculateBenfordScore(List<Double> priceChanges) {
        if (priceChanges.isEmpty()) return 1.0; // Pas de données, donc suspicion

        Map<Integer, Integer> digitCount = new HashMap<>();

        // Compter les premiers chiffres significatifs
        for (double change : priceChanges) {
            int leadingDigit = getFirstSignificantDigit(change);
            digitCount.put(leadingDigit, digitCount.getOrDefault(leadingDigit, 0) + 1);
        }

        // Convertir en distribution relative
        int total = priceChanges.size();
        double score = 0.0;

        for (int i = 1; i <= 9; i++) {
            double observedFrequency = digitCount.getOrDefault(i, 0) / (double) total;
            double expectedFrequency = BENFORD_DISTRIBUTION[i];

            // Mesurer l'écart entre les distributions (MSE)
            score += Math.pow(observedFrequency - expectedFrequency, 2);
        }

        return score; // Plus le score est élevé, plus l'anomalie est grande
    }

    /**
     * Récupère le premier chiffre significatif d'un nombre.
     */
    private int getFirstSignificantDigit(double number) {
        number = Math.abs(number);
        while (number >= 10) number /= 10;
        while (number < 1 && number > 0) number *= 10;
        return (int) number;
    }
    
    @Override
    public int evaluate(TradeRequestDTO tradeRequest, List<CandleDTO> candles) {
        if (candles.size() < 2) return 0;
        List<Double> changes = new java.util.ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            double diff = candles.get(i).getClose().doubleValue() - candles.get(i-1).getClose().doubleValue();
            changes.add(Math.abs(diff));
        }
        double score = calculateBenfordScore(changes);
        return (score > THRESHOLD) ? 0 : 1;
    }
}
