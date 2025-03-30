package finance.project.api.filters.rules;

import finance.project.api.entities.HighLowSwing;
import finance.project.api.entities.PointOfInterest;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service

public class ICTPointOfInterestFilter {


    private final MarketDataService marketDataService;

    @Autowired
    public ICTPointOfInterestFilter(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

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
        points.addAll(detectFibonacciRetracements(candles));
        points.addAll(detectOrderBlocks(candles));
        points.addAll(detectPsychologicalLevels(candles));
        points.addAll(detectVolumeProfileLevels(candles));
        points.addAll(detectBreakawayGaps(candles));
        points.addAll(detectContinuationGaps(candles));

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

    private List<HighLowSwing> detectSwingPoints(List<CandleDTO> candles, int windowSize) {
        List<HighLowSwing> swings = new ArrayList<>();
        int halfWindow = windowSize / 2;

        for (int i = halfWindow; i < candles.size() - halfWindow; i++) {
            List<CandleDTO> window = candles.subList(i - halfWindow, i + halfWindow + 1);
            CandleDTO current = candles.get(i);

            // Identifier les sommets locaux
            boolean isLocalHigh = true;
            for (CandleDTO candle : window) {
                if (candle.getHigh().doubleValue() > current.getHigh().doubleValue()) {
                    isLocalHigh = false;
                    break;
                }
            }

            // Identifier les creux locaux
            boolean isLocalLow = true;
            for (CandleDTO candle : window) {
                if (candle.getLow().doubleValue() < current.getLow().doubleValue()) {
                    isLocalLow = false;
                    break;
                }
            }

            if (isLocalHigh) {
                swings.add(new HighLowSwing(current.getHigh().doubleValue(), current.getDate(), true));
            }
            if (isLocalLow) {
                swings.add(new HighLowSwing(current.getLow().doubleValue(), current.getDate(), false));
            }
        }
        return swings;
    }


    private List<PointOfInterest> detectFibonacciRetracements(List<CandleDTO> candles) {
        List<PointOfInterest> fibLevels = new ArrayList<>();
        List<HighLowSwing> swings = detectSwingPoints(candles, 10);

        for (int i = 1; i < swings.size(); i++) {
            HighLowSwing prevSwing = swings.get(i - 1);
            HighLowSwing currentSwing = swings.get(i);

            double high = Math.max(prevSwing.getHigh(), currentSwing.getHigh());
            double low = Math.min(prevSwing.getLow(), currentSwing.getLow());

            double fib38 = low + (high - low) * 0.382;
            double fib50 = low + (high - low) * 0.5;
            double fib618 = low + (high - low) * 0.618;

            fibLevels.add(PointOfInterest.builder()
                    .name("Fibonacci 38.2%")
                    .symbol(candles.getFirst().getSymbol().getSymbol())
                    .type(8)
                    .high(fib38)
                    .low(fib38)
                    .timeframe(candles.getFirst().getTimeframe())
                    .valid(true)
                    .datetime(candles.getFirst().getDate())
                    .count(0)
                    .combled(0.0)
                    .signature(false)
                    .signatureType(0)
                    .signatureNameType("None")
                    .build());

            fibLevels.add(PointOfInterest.builder()
                    .name("Fibonacci 50%")
                    .symbol(candles.getFirst().getSymbol().getSymbol())
                    .type(8)
                    .high(fib50)
                    .low(fib50)
                    .timeframe(candles.getFirst().getTimeframe())
                    .valid(true)
                    .datetime(candles.getFirst().getDate())
                    .count(0)
                    .combled(0.0)
                    .signature(false)
                    .signatureType(0)
                    .signatureNameType("None")
                    .build());

            fibLevels.add(PointOfInterest.builder()
                    .name("Fibonacci 61.8%")
                    .symbol(candles.getFirst().getSymbol().getSymbol())
                    .type(8)
                    .high(fib618)
                    .low(fib618)
                    .timeframe(candles.getFirst().getTimeframe())
                    .valid(true)
                    .datetime(candles.getFirst().getDate())
                    .count(0)
                    .combled(0.0)
                    .signature(false)
                    .signatureType(0)
                    .signatureNameType("None")
                    .build());
        }
        return fibLevels;
    }

    private List<PointOfInterest> detectBreakawayGaps(List<CandleDTO> candles) {
        List<PointOfInterest> gaps = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            CandleDTO prevCandle = candles.get(i - 1);
            CandleDTO currentCandle = candles.get(i);
            double highLowRange = prevCandle.getHigh().doubleValue() - prevCandle.getLow().doubleValue();

            if (Math.abs(currentCandle.getOpen().doubleValue() - prevCandle.getClose().doubleValue()) > highLowRange * 1.5) {
                gaps.add(PointOfInterest.builder()
                        .name("Breakaway Gap")
                        .symbol(currentCandle.getSymbol().getName())
                        .type(7)
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

    private List<PointOfInterest> detectContinuationGaps(List<CandleDTO> candles) {
        List<PointOfInterest> gaps = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            CandleDTO prev = candles.get(i - 1);
            CandleDTO current = candles.get(i);
            if (Math.abs(current.getOpen().doubleValue() - prev.getClose().doubleValue()) > Math.abs(prev.getHigh().doubleValue()-prev.getLow().doubleValue()) * 1.2 &&
                    current.getVolume().doubleValue() > prev.getVolume().doubleValue()) {
                gaps.add(PointOfInterest.builder()
                        .name("Continuation Gap")
                        .symbol(current.getSymbol().getName())
                        .type(7)
                        .high(current.getOpen().doubleValue())
                        .low(prev.getClose().doubleValue())
                        .timeframe(current.getTimeframe())
                        .valid(true)
                        .datetime(current.getDate())
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

    private List<PointOfInterest> detectOrderBlocks(List<CandleDTO> candles) {
        List<PointOfInterest> orderBlocks = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            CandleDTO prev = candles.get(i - 1);
            CandleDTO current = candles.get(i);
            if (prev.getClose().doubleValue() < prev.getOpen().doubleValue() && current.getClose().doubleValue() > current.getOpen().doubleValue()) {
                orderBlocks.add(PointOfInterest.builder()
                        .name("Bullish Order Block")
                        .symbol(prev.getSymbol().getName())
                        .type(1) // 1 for Bullish Order Block
                        .high(prev.getClose().doubleValue())
                        .low(prev.getLow().doubleValue())
                        .timeframe(current.getTimeframe())
                        .valid(true)
                        .datetime(prev.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
            if (prev.getClose().doubleValue() > prev.getOpen().doubleValue() && current.getClose().doubleValue() < current.getOpen().doubleValue()) {
                orderBlocks.add(PointOfInterest.builder()
                        .name("Bearish Order Block")
                        .symbol(prev.getSymbol().getName())
                        .type(2) // 2 for Bearish Order Block
                        .high(prev.getClose().doubleValue())
                        .low(prev.getLow().doubleValue())
                        .timeframe(current.getTimeframe())
                        .valid(true)
                        .datetime(prev.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
        }
        return orderBlocks;
    }

    private List<PointOfInterest> detectPsychologicalLevels(List<CandleDTO> candles) {
        List<PointOfInterest> levels = new ArrayList<>();
        for (CandleDTO candle : candles) {
            double price = candle.getClose().doubleValue();
            if (price % 100 == 0 || price % 50 == 0) {
                levels.add(PointOfInterest.builder()
                        .name("Psychological Level")
                        .symbol(candle.getSymbol().getName())
                        .type(3)
                        .high(price)
                        .low(price)
                        .timeframe(candle.getTimeframe())
                        .valid(true)
                        .datetime(candle.getDate())
                        .count(0)
                        .combled(0.0)
                        .signature(false)
                        .signatureType(0)
                        .signatureNameType("None")
                        .build());
            }
        }
        return levels;
    }

    private List<PointOfInterest> detectVolumeProfileLevels(List<CandleDTO> candles) {
        List<PointOfInterest> levels = new ArrayList<>();
        double poc = marketDataService.calculatePOC(candles);
        double vah = marketDataService.calculateVAH(candles);
        double val = marketDataService.calculateVAL(candles);
        levels.add(PointOfInterest.builder()
                .name("Point of Control (POC)")
                .symbol(candles.get(candles.size() - 1).getSymbol().getName())
                .type(4)
                .high(poc)
                .low(poc)
                .timeframe(candles.get(candles.size() - 1).getTimeframe())
                .valid(true)
                .datetime(candles.get(candles.size() - 1).getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build());
        levels.add(PointOfInterest.builder()
                .name("Value Area High (VAH)")
                .symbol(candles.get(candles.size() - 1).getSymbol().getName())
                .type(5)
                .high(vah)
                .low(vah)
                .timeframe(candles.get(candles.size() - 1).getTimeframe())
                .valid(true)
                .datetime(candles.get(candles.size() - 1).getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build());
        levels.add(PointOfInterest.builder()
                .name("Value Area Low (VAL)")
                .symbol(candles.get(candles.size() - 1).getSymbol().getName())
                .type(6)
                .high(val)
                .low(val)
                .timeframe(candles.get(candles.size() - 1).getTimeframe())
                .valid(true)
                .datetime(candles.get(candles.size() - 1).getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build());
        return levels;
    }

    private PointOfInterest createFibonacciPoint(CandleDTO highCandle, CandleDTO lowCandle, String name, double level) {
        return PointOfInterest.builder()
                .name(name)
                .symbol(highCandle.getSymbol().getName())
                .type(6) // Type spécifique pour retracements
                .high(level)
                .low(level)
                .timeframe(highCandle.getTimeframe())
                .valid(true)
                .datetime(highCandle.getDate())
                .count(0)
                .combled(0.0)
                .signature(false)
                .signatureType(0)
                .signatureNameType("None")
                .build();
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

