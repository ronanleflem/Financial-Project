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

        // Détection des gaps institutionnels
        points.addAll(detectNewsOpenGaps(candles));

        // Détection des gaps normaux entre bougies
        points.addAll(detectNormalGaps(candles));

        // Détection des Fair Value Gaps (FVG)
        points.addAll(detectFairValueGaps(candles));

        // Détection des Previous Highs & Lows
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
                double gap = Math.abs(currentCandle.getOpen().doubleValue() - prevCandle.getClose().doubleValue());

                int type = lastTimeframe.equals("D") ? 0 : 1;
                String name = lastTimeframe.equals("D") ? "News Daily Open Gap" : "News Weekly Open Gap";

                gaps.add(new PointOfInterest(name, type,
                        currentCandle.getOpen().doubleValue(),
                        prevCandle.getClose().doubleValue(),
                        true, currentCandle.getDate(),
                        0, 0, false, 0, "None"
                ));
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
            if (gap > 0.0002) { // Seuil pour éviter les faux gaps
                gaps.add(new PointOfInterest( "Normal Gap", 2,
                        currentCandle.getOpen().doubleValue(),
                        prevCandle.getClose().doubleValue(),
                        true,  currentCandle.getDate(),
                        0, 0, false, 0, "None"
                ));


            }
        }
        return gaps;
    }

    /**
     * Détecte les Fair Value Gaps (FVG).
     */
    private List<PointOfInterest> detectFairValueGaps(List<CandleDTO> candles) {
        List<PointOfInterest> fvgPoints = new ArrayList<>();
        for (int i = 2; i < candles.size(); i++) {
            CandleDTO c1 = candles.get(i - 2);
            CandleDTO c3 = candles.get(i);

            if (c3.getLow().doubleValue() > c1.getHigh().doubleValue()) {
                fvgPoints.add(new PointOfInterest("Fair Value Gap", 3,
                        c1.getHigh().doubleValue(),
                        c3.getLow().doubleValue(),
                        true, c3.getDate(),
                        0, 0, false, 0, "None"
                ));
            }

            if (c3.getHigh().doubleValue() < c1.getLow().doubleValue()) {
                fvgPoints.add(new PointOfInterest("Inverted Fair Value Gap",4,
                        c3.getHigh().doubleValue(),
                        c1.getLow().doubleValue(),
                        true, c3.getDate(),
                        0, 0, false, 0, "None"
                ));
            }
        }
        return fvgPoints;
    }

    /**
     * Détecte les Previous Highs & Lows (Daily, Weekly, Monthly).
     */
    private List<PointOfInterest> detectPreviousHighsLows(List<CandleDTO> candles) {
        List<PointOfInterest> previousLevels = new ArrayList<>();
        double prevDailyHigh = 0, prevDailyLow = 0;
        double prevWeeklyHigh = 0, prevWeeklyLow = 0;
        double prevMonthlyHigh = 0, prevMonthlyLow = 0;

        for (CandleDTO candle : candles) {
            if (candle.getTimeframe().equals("D")) {
                prevDailyHigh = candle.getHigh().doubleValue();
                prevDailyLow = candle.getLow().doubleValue();
                previousLevels.add(new PointOfInterest( "Prev Daily High",5,prevDailyHigh, null, true,  candle.getDate(), 0, 0, false, 0, "None"));
                previousLevels.add(new PointOfInterest("Prev Daily Low",5,prevDailyLow, null, true, candle.getDate(), 0, 0, false, 0, "None"));
            }
            if (candle.getTimeframe().equals("W")) {
                prevWeeklyHigh = candle.getHigh().doubleValue();
                prevWeeklyLow = candle.getLow().doubleValue();
                previousLevels.add(new PointOfInterest( "Prev Weekly High",5,prevWeeklyHigh, null, true,  candle.getDate(), 0, 0, false, 0, "None"));
                previousLevels.add(new PointOfInterest("Prev Weekly Low",5,prevWeeklyLow, null, true, candle.getDate(), 0, 0, false, 0, "None"));
            }
            if (candle.getTimeframe().equals("M")) {
                prevMonthlyHigh = candle.getHigh().doubleValue();
                prevMonthlyLow = candle.getLow().doubleValue();
                previousLevels.add(new PointOfInterest( "Prev Monthly High",5,prevMonthlyHigh, null, true,  candle.getDate(), 0, 0, false, 0, "None"));
                previousLevels.add(new PointOfInterest("Prev Monthly Low",5,prevMonthlyLow, null, true, candle.getDate(), 0, 0, false, 0, "None"));
            }
        }
        return previousLevels;
    }
}
