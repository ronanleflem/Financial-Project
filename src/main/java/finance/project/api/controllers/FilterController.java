package finance.project.api.controllers;

import finance.project.api.filters.rules.CandleStructureFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
public class FilterController {

    private static final List<String> TIMEFRAMES = List.of("1min", "3min", "5min", "15min", "30min", "1h", "4h", "daily", "weekly", "monthly");

    private final CandleStructureFilter candleStructureFilter;

    @GetMapping("/bullish-bearish-stats")
    public ResponseEntity<Map<String, Double>> getBullishContinuationProbability(@RequestParam String symbol, @RequestParam String timeframe) {
        Map<String, Double> probability = candleStructureFilter.calculateContinuationProbabilities(symbol,timeframe);
        return ResponseEntity.ok(probability);
    }

    @GetMapping("/bullish-bearish-stats/all")
    public ResponseEntity<Map<String, Map<String, Double>>> getAllBullishContinuationProbability(
            @RequestParam String symbol) {

        Map<String, Map<String, Double>> result = new HashMap<>();

        for (String tf : TIMEFRAMES) {
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf));
        }

        return ResponseEntity.ok(result);
    }
}
