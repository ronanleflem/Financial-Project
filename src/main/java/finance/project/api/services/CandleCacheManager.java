package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CandleCacheManager {

    @Autowired
    private CandleService candleService;

    private final Map<String, List<CandleDTO>> cache = new HashMap<>();

    public List<CandleDTO> getCandles(String symbol, String timeframe, int period) {
        String key = symbol + "_" + timeframe + "_" + period;
        return cache.computeIfAbsent(key, k -> candleService.getLastCandles(symbol, timeframe, period));
    }

    public List<CandleDTO> getCandles(String symbol, String timeframe, LocalDateTime startDate, LocalDateTime endDate) {
        String key = symbol + "_" + timeframe + "_" + startDate + "_" + endDate;
        return cache.computeIfAbsent(key, k ->
                candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate)
        );
    }

    public void preload(String symbol, String timeframe, int period) {
        String key = symbol + "_" + timeframe + "_" + period;
        if (!cache.containsKey(key)) {
            cache.put(key, candleService.getLastCandles(symbol, timeframe, period));
        }
    }

    public void preload(String symbol, String timeframe, LocalDateTime startDate, LocalDateTime endDate) {
        String key = symbol + "_" + timeframe + "_" + startDate + "_" + endDate;
        if (!cache.containsKey(key)) {
            cache.put(key, candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate));
        }
    }

    public void clearCache() {
        cache.clear();
    }
}