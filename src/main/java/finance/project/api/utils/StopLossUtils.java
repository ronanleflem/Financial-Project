package finance.project.api.utils;

import finance.project.api.model.CandleDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StopLossUtils {

    /**
     * Computes the stop loss price for a long trade by looking at the lowest
     * low among the last {@code lookback} candles.
     *
     * @param candles  list of candles ordered from oldest to newest
     * @param lookback number of candles to consider
     * @return the lowest low in the lookback window or 0 if candles are empty
     */
    public static double computeLongStopLoss(List<CandleDTO> candles, int lookback) {
        if (candles == null || candles.isEmpty()) {
            return 0;
        }
        int fromIndex = Math.max(0, candles.size() - lookback);
        return candles.subList(fromIndex, candles.size()).stream()
                .map(CandleDTO::getLow)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .min()
                .orElseGet(() -> candles.get(candles.size() - 1).getLow().doubleValue());
    }

    /**
     * Computes the stop loss price for a short trade by looking at the highest
     * high among the last {@code lookback} candles.
     *
     * @param candles  list of candles ordered from oldest to newest
     * @param lookback number of candles to consider
     * @return the highest high in the lookback window or 0 if candles are empty
     */
    public static double computeShortStopLoss(List<CandleDTO> candles, int lookback) {
        if (candles == null || candles.isEmpty()) {
            return 0;
        }
        int fromIndex = Math.max(0, candles.size() - lookback);
        return candles.subList(fromIndex, candles.size()).stream()
                .map(CandleDTO::getHigh)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElseGet(() -> candles.get(candles.size() - 1).getHigh().doubleValue());
    }
}
