package finance.project.api.controllers;

import finance.project.api.filters.rules.CandleStructureFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
public class FilterController {

    private final CandleStructureFilter candleStructureFilter;

    @GetMapping("/bullish-continuation")
    public ResponseEntity<Map<String, Double>> getBullishContinuationProbability(@RequestParam String symbol, @RequestParam String timeframe) {
        Map<String, Double> probability = candleStructureFilter.calculateContinuationProbabilities(symbol,timeframe);
        return ResponseEntity.ok(probability);
    }
}
