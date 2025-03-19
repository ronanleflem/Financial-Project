package finance.project.api.controllers;

import finance.project.api.filters.rules.*;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.CandleService;
import finance.project.api.services.MarketDataService;
import finance.project.api.services.OrderFlowService;
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
    private static final List<String> TIMEFRAMESVOLCME = List.of("5min", "15min", "30min", "1h", "4h","daily");

    private final CandleStructureFilter candleStructureFilter;
    private final BenfordLawFilter benfordLawFilter;
    private final BiaisInstitutionalFilter biasInstitutionalFilter;
    private final ContradictorySignalsFilter contradictorySignalsFilter;
    private final CyclesFilter cyclesFilter;
    private final EntropyMarketFilter entropyMarketFilter;
    private final FractalAnalysisFilter fractalAnalysisFilter;
    private final MarketManipulationFilter marketManipulationFilter;
    private final HighTimeframeZoneFilter highTimeframeZoneFilter;
    private final DonchianChannelsFilter donchianChannelsFilter;
    private final LiquidityFilter liquidityFilter;
    private final LowerTimeframeConfluenceFilter lowerTimeframeConfluenceFilter;

    private final CandleService candleService;
    private final MarketDataService marketDataService;
    private final OrderFlowService orderFlowService;

    @GetMapping("/bullish-bearish-stats")
    public ResponseEntity<Map<String, String>> getBullishContinuationProbability(@RequestParam String symbol, @RequestParam String timeframe) {
        Map<String, String> probability = candleStructureFilter.calculateContinuationProbabilities(symbol,timeframe, -1);
        return ResponseEntity.ok(probability);
    }

    @GetMapping("/bullish-bearish-stats/all")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbability(
            @RequestParam String symbol) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMESVOLCME) {
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf, -1));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/bullish-bearish-stats/all/limit")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbabilityLimit(
            @RequestParam String symbol,  @RequestParam Integer numberLastestCandles) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMESVOLCME) {
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

    @GetMapping("/institutional-biais")
    public ResponseEntity<Map<String, Integer>> getInstitutionalBias(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int maxCandle) {

        // Récupération des 1000 dernières bougies
        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, maxCandle);

        // Calcul du biais institutionnel
        int bias = biasInstitutionalFilter.calculateInstitutionalBias(candlesLatest);

        Map<String, Integer> result = new HashMap<>();
        result.put("institutionalBias", bias);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/contradictory-signals")
    public ResponseEntity<Map<String, Integer>> getContradictionScore(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int maxCandle) {

        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, maxCandle);

        // Récupération des indicateurs
        //double price = marketDataService.getCurrentPrice(symbol);
        double price = candlesLatest.getFirst().getClose().doubleValue(); // Fixme : VAL TEMPORAIRE
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
            @RequestParam String symbol, @RequestParam String timeframe,@RequestParam int maxCandle) {

        // Récupération des variations de prix
        List<Double> prices = candleService.getPriceVariations(symbol, timeframe, maxCandle);

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
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int maxCandle) {

        List<Double> priceChanges = candleService.getPriceVariations(symbol, timeframe, maxCandle);
        double entropy = entropyMarketFilter.calculateMarketEntropy(priceChanges);

        Map<String, Double> result = new HashMap<>();
        result.put("entropy", entropy);

        return ResponseEntity.ok(result);
    }

    /**
     * TO FIXME
     * @param symbol
     * @param timeframe
     * @param maxCandle
     * @return
     */
    @GetMapping("/fractal-analysis")
    public ResponseEntity<Map<String, Double>> getFractalAnalysis(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int maxCandle) {

        // Récupération des variations de prix sous forme de rendements
        List<Double> returns = candleService.getPriceReturns(symbol, timeframe, maxCandle);

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
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int maxCandle) {

        List<Double> priceChanges = candleService.getPriceVariations(symbol, timeframe, maxCandle);
        int manipulationScore = marketManipulationFilter.detectManipulationZone(priceChanges);

        Map<String, Integer> result = new HashMap<>();
        result.put("manipulationScore", manipulationScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-timeframe-zones")
    public ResponseEntity<Map<String, Integer>> getHighTimeframeZones(
            @RequestParam String symbol, @RequestParam String timeframe) {

        // Récupération du prix actuel et des niveaux institutionnels
        double price = candleService.getLastCandles(symbol,"1min",1).get(0).getClose().doubleValue(); // Plus petite bougie pour récupèrer le prix le plus proches
        List<Double> keyLevels = null;//candleService.getInstitutionalLevels(symbol,timeframe);

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
        double price = candleService.getLastCandles(symbol,"1min",1).get(0).getClose().doubleValue();
        List<Double> keyLevels = null;//candleService.getInstitutionalLevels(symbol); // IL FAUT QUE JE CREE UNE TABLE POUR SA

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

        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, period);
        // Extraction des closes, highs et lows
        List<Double> highs = candlesLatest.stream().map(c -> c.getHigh().doubleValue()).toList();
        List<Double> lows = candlesLatest.stream().map(c -> c.getLow().doubleValue()).toList();

        // Calcul des Donchian Channels
        double[] donchianBands = donchianChannelsFilter.calculateDonchianBands(highs, lows);

        Map<String, Double> result = new HashMap<>();
        result.put("Upper Band", donchianBands[0]);
        result.put("Lower Band", donchianBands[1]);
        result.put("Middle Band", donchianBands[2]);

        return ResponseEntity.ok(result);
    }

    /**
     *
     * pRESSION ACHETEUSE VENDEUSE
     * @param symbol
     * @param timeframe
     * @param period
     * @return
     */
    @GetMapping("/liquidity")
    public ResponseEntity<Map<String, Double>> getMarketLiquidity(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(defaultValue = "20") int period) {

        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, period);
        // Extraction des closes, highs et lows
        List<Double> closes = candlesLatest.stream().map(c -> c.getClose().doubleValue()).toList();
        List<Double> highs = candlesLatest.stream().map(c -> c.getHigh().doubleValue()).toList();
        List<Double> lows = candlesLatest.stream().map(c -> c.getLow().doubleValue()).toList();
        List<Double> volumes = candlesLatest.stream().map(c -> c.getVolume().doubleValue()).toList();

        // Calcul du CMF
        double cmf = liquidityFilter.calculateCMF(closes, highs, lows, volumes);

        Map<String, Double> result = new HashMap<>();
        result.put("CMF", cmf);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/lower-timeframe-confluence")
    public ResponseEntity<Map<String, Integer>> getLowerTimeframeConfluence(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int period) {

        // Récupération des prix et indicateurs

        List<CandleDTO> candlesLatest = candleService.getLastCandles(symbol, timeframe, period);
        // Extraction des closes, highs et lows
        List<Double> closes = candlesLatest.stream().map(c -> c.getClose().doubleValue()).toList();
        List<Double> highs = candlesLatest.stream().map(c -> c.getHigh().doubleValue()).toList();
        List<Double> lows = candlesLatest.stream().map(c -> c.getLow().doubleValue()).toList();



        double momentum = lowerTimeframeConfluenceFilter.calculateMomentum(closes, 10);
        double adx = lowerTimeframeConfluenceFilter.calculateADX(highs, lows, closes, 14);
        boolean trendAligned = lowerTimeframeConfluenceFilter.isTrendAligned(
                marketDataService.calculateEMA(candlesLatest,  20),
                marketDataService.calculateEMA(candlesLatest, 50),
                marketDataService.calculateEMA(candlesLatest, 200)
        );

        double vwapDistance = marketDataService.calculateVWAP(candlesLatest) - closes.get(closes.size() - 1);
        double deltaVolume = orderFlowService.getDeltaVolume(symbol, "M5"); // FIXME : faire en sorte d'avoir les volumes pour VWAP

        // Calcul du score de confluence
        int confluenceScore = lowerTimeframeConfluenceFilter.calculateConfluenceScore(momentum, adx, trendAligned, vwapDistance, deltaVolume);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }
}
