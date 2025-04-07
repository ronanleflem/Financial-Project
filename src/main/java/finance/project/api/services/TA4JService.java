package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TA4JService {

    public BarSeries convertToTimeSeries(List<CandleDTO> candles, String timeframe) {
        BarSeries series = new BaseBarSeriesBuilder().withName("Candle Series (" + timeframe + ")").build();
        Duration barDuration = parseTimeframe(timeframe);

        for (CandleDTO candle : candles) {
            ZonedDateTime zdt = candle.getDate().atZone(ZoneId.systemDefault());

            series.addBar(new BaseBar(
                    barDuration,
                    zdt,
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    candle.getVolume()
            ));
        }

        return series;
    }

    private Duration parseTimeframe(String timeframe) {
        if (timeframe == null || timeframe.isBlank()) return Duration.ofMinutes(1); // par défaut M1

        String tf = timeframe.toLowerCase();
        if (tf.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(tf.replace("m", "")));
        } else if (tf.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(tf.replace("h", "")));
        } else if (tf.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(tf.replace("d", "")));
        } else if (tf.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(tf.replace("s", "")));
        }

        throw new IllegalArgumentException("Timeframe non reconnu : " + timeframe);
    }




    public double calculateATR(BarSeries series, int period) {
        ATRIndicator atr = new ATRIndicator(series, period);
        return atr.getValue(series.getEndIndex()).doubleValue();
    }

    public double[] calculateBollingerBands(BarSeries series, int period, double multiplier) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, period);

        double middleBand = sma.getValue(series.getEndIndex()).doubleValue();
        double upperBand = middleBand + (multiplier * stdDev.getValue(series.getEndIndex()).doubleValue());
        double lowerBand = middleBand - (multiplier * stdDev.getValue(series.getEndIndex()).doubleValue());

        return new double[]{lowerBand, middleBand, upperBand};
    }
}
