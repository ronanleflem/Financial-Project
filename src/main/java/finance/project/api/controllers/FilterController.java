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

    @GetMapping("/cycles")
    public ResponseEntity<Map<String, Double>> getMarketCycles(
            @RequestParam String symbol, @RequestParam String timeframe) {

        // Récupération des variations de prix
        List<Double> prices = candleService.getPriceVariations(symbol, timeframe, 200);

        // Calcul du coefficient de détermination R²
        double rSquared = cyclesFilter.calculateR2(prices);

        // Détection du cycle dominant (en nombre de bougies)
        int dominantCycle = cyclesFilter.detectDominantCycle(prices);

        Map<String, Double> result = new HashMap<>();
        result.put("rSquared", rSquared);
        result.put("dominantCycle", (double) dominantCycle); // Converti en Double pour le JSON

        return ResponseEntity.ok(result);
    }

    @GetMapping("/entropy")
    public ResponseEntity<Map<String, Double>> getMarketEntropy(
            @RequestParam String symbol, @RequestParam String timeframe) {

        List<Double> priceChanges = candleService.getPriceVariations(symbol, timeframe, 200);
        double entropy = entropyMarketFilter.calculateMarketEntropy(priceChanges);

        Map<String, Double> result = new HashMap<>();
        result.put("entropy", entropy);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/fractal-analysis")
    public ResponseEntity<Map<String, Double>> getFractalAnalysis(
            @RequestParam String symbol, @RequestParam String timeframe) {

        // Récupération des variations de prix sous forme de rendements
        List<Double> returns = candleService.getPriceReturns(symbol, timeframe, 200);

        // Calcul du Ratio de Hurst
        double hurstExponent = fractalAnalysisFilter.calculateHurstExponent(returns);

        // Calcul de la Kurtosis et Skewness
        double kurtosis = fractalAnalysisFilter.calculateKurtosis(returns);
        double skewness = fractalAnalysisFilter.calculateSkewness(returns);

        Map<String, Double> result = new HashMap<>();
        result.put("hurstExponent", hurstExponent);
        result.put("kurtosis", kurtosis);
        result.put("skewness", skewness);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/market-manipulation")
    public ResponseEntity<Map<String, Integer>> detectMarketManipulation(
            @RequestParam String symbol, @RequestParam String timeframe) {

        List<Double> priceChanges = candleService.getPriceVariations(symbol, timeframe, 200);
        int manipulationScore = marketManipulationFilter.detectManipulationZone(priceChanges);

        Map<String, Integer> result = new HashMap<>();
        result.put("manipulationScore", manipulationScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-timeframe-zones")
    public ResponseEntity<Map<String, Integer>> getHighTimeframeZones(
            @RequestParam String symbol) {

        // Récupération du prix actuel et des niveaux institutionnels
        double price = candleService.getCurrentPrice(symbol);
        List<Double> keyLevels = candleService.getInstitutionalLevels(symbol);

        // Calcul du score de confluence
        int confluenceScore = highTimeframeZoneFilter.checkInstitutionalConfluence(price, keyLevels);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-timeframe-zones-with-orderflow")
    public ResponseEntity<Map<String, Integer>> getHighTimeframeZonesWithOrderFlow(
            @RequestParam String symbol) {

        // Récupération des prix et zones institutionnelles
        double price = candleService.getCurrentPrice(symbol);
        List<Double> keyLevels = candleService.getInstitutionalLevels(symbol);

        // Récupération du flux d’ordres
        List<Double> buyVolumes = orderFlowService.getBuyVolumes(symbol, keyLevels);
        List<Double> sellVolumes = orderFlowService.getSellVolumes(symbol, keyLevels);

        // Calcul du score final
        int confluenceScore = highTimeframeZoneFilter.checkInstitutionalConfluenceWithOrderFlow(price, keyLevels, buyVolumes, sellVolumes);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/donchian-channels")
    public ResponseEntity<Map<String, Double>> getDonchianChannels(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(defaultValue = "20") int period) {

        // Récupération des prix hauts et bas sur la période demandée
        List<Double> highs = candleService.getHighs(symbol, timeframe, period);
        List<Double> lows = candleService.getLows(symbol, timeframe, period);

        // Calcul des Donchian Channels
        double[] donchianBands = donchianChannelsFilter.calculateDonchianBands(highs, lows);

        Map<String, Double> result = new HashMap<>();
        result.put("Upper Band", donchianBands[0]);
        result.put("Lower Band", donchianBands[1]);
        result.put("Middle Band", donchianBands[2]);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/liquidity")
    public ResponseEntity<Map<String, Double>> getMarketLiquidity(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(defaultValue = "20") int period) {

        // Récupération des données de marché
        List<Double> closes = candleService.getCloses(symbol, timeframe, period);
        List<Double> highs = candleService.getHighs(symbol, timeframe, period);
        List<Double> lows = candleService.getLows(symbol, timeframe, period);
        List<Double> volumes = candleService.getVolumes(symbol, timeframe, period);

        // Calcul du CMF
        double cmf = liquidityFilter.calculateCMF(closes, highs, lows, volumes);

        Map<String, Double> result = new HashMap<>();
        result.put("CMF", cmf);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/lower-timeframe-confluence")
    public ResponseEntity<Map<String, Integer>> getLowerTimeframeConfluence(
            @RequestParam String symbol) {

        // Récupération des prix et indicateurs
        List<Double> closes = candleService.getCloses(symbol, "M5", 50);
        List<Double> highs = candleService.getHighs(symbol, "M5", 50);
        List<Double> lows = candleService.getLows(symbol, "M5", 50);

        double momentum = lowerTimeframeConfluenceFilter.calculateMomentum(closes, 10);
        double adx = lowerTimeframeConfluenceFilter.calculateADX(highs, lows, closes, 14);
        boolean trendAligned = lowerTimeframeConfluenceFilter.isTrendAligned(
                marketDataService.getEMA(symbol, "M5", 20),
                marketDataService.getEMA(symbol, "M5", 50),
                marketDataService.getEMA(symbol, "M5", 200)
        );

        double vwapDistance = marketDataService.getVWAP(symbol, "M5") - closes.get(closes.size() - 1);
        double deltaVolume = orderFlowService.getDeltaVolume(symbol, "M5");

        // Calcul du score de confluence
        int confluenceScore = lowerTimeframeConfluenceFilter.calculateConfluenceScore(momentum, adx, trendAligned, vwapDistance, deltaVolume);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }
}
