package finance.project.api.filters.rules;

import finance.project.api.entities.PointOfInterest;
import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ICTPointOfInterestFilter {

    /**
     * Analyse une liste de bougies et retourne tous les points d'intérêt ICT.
     *
     * @param candles Liste des bougies (du plus ancien au plus récent)
     * @return Liste des points d'intérêt
     */
    public List<PointOfInterest> analyzeICTPoints(List<CandleDTO> candles) {
        if (candles.size() < 3) {
            throw new IllegalArgumentException("Il faut au moins 3 bougies pour détecter les points ICT.");
        }

        List<PointOfInterest> points = new ArrayList<>();

        points.addAll(detectNewsOpenGaps(candles));
        //points.addAll(detectNormalGaps(candles));
        points.addAll(detectFairValueGaps(candles));
        points.addAll(detectPreviousHighsLows(candles));

        return points;
    }

    /**
     * Détecte les gaps institutionnels (Daily/Weekly Open Gaps).
     */
    private List<PointOfInterest> detectNewsOpenGaps(List<CandleDTO> candles) {
        List<PointOfInterest> gaps = new ArrayList<>();
        String lastTimeframe = "";

        for (int i = 1; i < candles.size(); i++) {
            CandleDTO prevCandle = candles.get(i - 1);
            CandleDTO currentCandle = candles.get(i);

            if (!currentCandle.getTimeframe().equals(lastTimeframe)) {
                lastTimeframe = currentCandle.getTimeframe();

                double gapSize = Math.abs(currentCandle.getOpen().doubleValue() - prevCandle.getClose().doubleValue());

                int type = lastTimeframe.equalsIgnoreCase("D") ? 0 : 1;
                String name = lastTimeframe.equalsIgnoreCase("D") ? "News Daily Open Gap" : "News Weekly Open Gap";

                gaps.add(PointOfInterest.builder()
                        .name(name)
                        .symbol(currentCandle.getSymbol().getName())
                        .type(type)
                        .high(Math.max(currentCandle.getOpen().doubleValue(), prevCandle.getClose().doubleValue()))
                        .low(Math.min(currentCandle.getOpen().doubleValue(), prevCandle.getClose().doubleValue()))
                        .timeframe(currentCandle.getTimeframe())
                        .valid(true)
                        .datetime(currentCandle.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
        }

        return gaps;
    }

    /**
     * Détecte les gaps normaux entre deux bougies consécutives.
     */
    private List<PointOfInterest> detectNormalGaps(List<CandleDTO> candles) {
        List<PointOfInterest> gaps = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            CandleDTO prevCandle = candles.get(i - 1);
            CandleDTO currentCandle = candles.get(i);

            double gap = Math.abs(currentCandle.getOpen().doubleValue() - prevCandle.getClose().doubleValue());

            if (gap > 0.0010) { // Seuil pour éviter les micro-gaps
                gaps.add(PointOfInterest.builder()
                        .name("Normal Gap")
                        .symbol(currentCandle.getSymbol().getName())
                        .type(2)
                        .high(Math.max(currentCandle.getOpen().doubleValue(), prevCandle.getClose().doubleValue()))
                        .low(Math.min(currentCandle.getOpen().doubleValue(), prevCandle.getClose().doubleValue()))
                        .timeframe(currentCandle.getTimeframe())
                        .valid(true)
                        .datetime(currentCandle.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
        }

        return gaps;
    }

    /**
     * Détecte les Fair Value Gaps (FVG) et Inverted FVG.
     */
    private List<PointOfInterest> detectFairValueGaps(List<CandleDTO> candles) {
        List<PointOfInterest> fvgPoints = new ArrayList<>();

        for (int i = 2; i < candles.size(); i++) {
            CandleDTO c1 = candles.get(i - 2);
            CandleDTO c3 = candles.get(i);

            // Fair Value Gap Bullish
            if (c3.getLow().doubleValue() > c1.getHigh().doubleValue()) {
                fvgPoints.add(PointOfInterest.builder()
                        .name("Fair Value Gap")
                        .symbol(c3.getSymbol().getName())
                        .type(3)
                        .high(c1.getHigh().doubleValue())
                        .low(c3.getLow().doubleValue())
                        .timeframe(c3.getTimeframe())
                        .valid(true)
                        .datetime(c3.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }

            // Fair Value Gap Bearish (Inverted)
            if (c3.getHigh().doubleValue() < c1.getLow().doubleValue()) {
                fvgPoints.add(PointOfInterest.builder()
                        .name("Inverted Fair Value Gap")
                        .symbol(c3.getSymbol().getName())
                        .type(4)
                        .high(c3.getHigh().doubleValue())
                        .low(c1.getLow().doubleValue())
                        .timeframe(c3.getTimeframe())
                        .valid(true)
                        .datetime(c3.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
        }

        return fvgPoints;
    }

    /**
     * Détecte les Previous Highs & Lows (Daily, Weekly, Monthly).
     */
    private List<PointOfInterest> detectPreviousHighsLows(List<CandleDTO> candles) {
        List<PointOfInterest> previousLevels = new ArrayList<>();

        for (CandleDTO candle : candles) {
            String tf = candle.getTimeframe();
            String symbol = candle.getSymbol().getName();

            if (tf.equalsIgnoreCase("D")) {
                previousLevels.addAll(createPrevLevels(symbol, tf, candle, "Daily", 5));
            } else if (tf.equalsIgnoreCase("W")) {
                previousLevels.addAll(createPrevLevels(symbol, tf, candle, "Weekly", 5));
            } else if (tf.equalsIgnoreCase("M")) {
                previousLevels.addAll(createPrevLevels(symbol, tf, candle, "Monthly", 5));
            }
        }

        return previousLevels;
    }

    /**
     * Helper : Crée les Previous High et Low pour un timeframe spécifique.
     */
    private List<PointOfInterest> createPrevLevels(String symbol, String timeframe, CandleDTO candle, String label, int type) {
        List<PointOfInterest> levels = new ArrayList<>();

        levels.add(PointOfInterest.builder()
                .name("Prev " + label + " High")
                .symbol(symbol)
                .type(type)
                .high(candle.getHigh().doubleValue())
                .low(null)
                .timeframe(timeframe)
                .valid(true)
                .datetime(candle.getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build());

        levels.add(PointOfInterest.builder()
                .name("Prev " + label + " Low")
                .symbol(symbol)
                .type(type)
                .high(candle.getLow().doubleValue())
                .low(null)
                .timeframe(timeframe)
                .valid(true)
                .datetime(candle.getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build());

        return levels;
    }
}

