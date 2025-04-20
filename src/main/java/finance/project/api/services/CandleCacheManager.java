package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void preload(String symbol, String timeframe, int period) {
        String key = symbol + "_" + timeframe + "_" + period;
        if (!cache.containsKey(key)) {
            cache.put(key, candleService.getLastCandles(symbol, timeframe, period));
        }
    }

    public void clearCache() {
        cache.clear();
    }
}