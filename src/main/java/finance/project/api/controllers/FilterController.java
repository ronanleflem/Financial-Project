package finance.project.api.controllers;

import finance.project.api.filters.rules.BenfordLawFilter;
import finance.project.api.filters.rules.BiaisInstitutionalFilter;
import finance.project.api.filters.rules.CandleStructureFilter;
import finance.project.api.filters.rules.ContradictorySignalsFilter;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.CandleService;
import finance.project.api.services.MarketDataService;
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

    private final BenfordLawFilter benfordLawFilter;

    private final BiaisInstitutionalFilter biasInstitutionalFilter;

    private final ContradictorySignalsFilter contradictorySignalsFilter;

    private final CandleService candleService;
    private final MarketDataService marketDataService;

    @GetMapping("/bullish-bearish-stats")
    public ResponseEntity<Map<String, String>> getBullishContinuationProbability(@RequestParam String symbol, @RequestParam String timeframe) {
        Map<String, String> probability = candleStructureFilter.calculateContinuationProbabilities(symbol,timeframe, -1);
        return ResponseEntity.ok(probability);
    }

    @GetMapping("/bullish-bearish-stats/all")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbability(
            @RequestParam String symbol) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMES) {
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf, -1));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/bullish-bearish-stats/all/limit")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbabilityLimit(
            @RequestParam String symbol,  @RequestParam Integer numberLastestCandles) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMES) {
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf, numberLastestCandles));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/benford/anomaly")
    public ResponseEntity<Map<String, Double>> getBenfordAnomalyScore(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam int numberLastestCandles) {

        // Récupération des variations de prix (exemple : différences entre open et close)
        List<Double> priceChanges = candleService.getPriceVariations(symbol, timeframe, numberLastestCandles);

        // Calcul du score de conformité
        double benfordScore = benfordLawFilter.calculateBenfordScore(priceChanges);

        Map<String, Double> result = new HashMap<>();
        result.put("benfordScore", benfordScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/institutional-bias")
    public ResponseEntity<Map<String, Integer>> getInstitutionalBias(
            @RequestParam String symbol, @RequestParam String timeframe) {

        // Récupération des 1000 dernières bougies
        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, 1000);

        // Calcul du biais institutionnel
        int bias = biasInstitutionalFilter.calculateInstitutionalBias(candlesLatest);

        Map<String, Integer> result = new HashMap<>();
        result.put("institutionalBias", bias);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/contradictory-signals")
    public ResponseEntity<Map<String, Integer>> getContradictionScore(
            @RequestParam String symbol, @RequestParam String timeframe) {

        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, 1000);

        // Récupération des indicateurs
        //double price = marketDataService.getCurrentPrice(symbol);
        double price = 1.04200; // Fixme : VAL TEMPORAIRE
        double ema50 = marketDataService.calculateEMA(candlesLatest,50);
        double ema200 = marketDataService.calculateEMA(candlesLatest,200);
        double rsi = marketDataService.calculateRSI(candlesLatest, 14);
        double macd = marketDataService.calculateMACD(candlesLatest, 12, 26);
        double macdSignal = marketDataService.calculateMACDSignal(candlesLatest, 12, 26, 9);
        double stochK = marketDataService.calculateStochasticK(candlesLatest, 14);
        double stochD = marketDataService.calculateStochasticD(candlesLatest, 14, 3);
        double zScore = marketDataService.calculateZScore(candlesLatest, 20);

        // Calcul du score de contradiction
        int contradictionScore = contradictorySignalsFilter.calculateContradictionScore(
                price, ema50, ema200, rsi, macd, macdSignal, stochK, stochD, zScore
        );

        Map<String, Integer> result = new HashMap<>();
        result.put("contradictionScore", contradictionScore);

        return ResponseEntity.ok(result);
    }
}
