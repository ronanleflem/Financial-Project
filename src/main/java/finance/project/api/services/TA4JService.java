package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

@Service
public class TA4JService {

    public BarSeries convertToTimeSeries(List<CandleDTO> candles,String timeframe) {
        BarSeries series = new BaseBarSeriesBuilder().withName("Candle Series").build();

        for (CandleDTO candle : candles) {
            series.addBar(
                    new BaseBar(
                            Duration.ofMinutes(1), // Modifier selon le timeframe
                            candle.getDate().atZone(ZoneId.systemDefault()),
                            candle.getOpen(),
                            candle.getHigh(),
                            candle.getLow(),
                            candle.getClose(),
                            candle.getVolume()
                    )
            );
        }
        return series;
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
