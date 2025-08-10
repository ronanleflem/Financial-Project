package finance.project.api.controllers;

import finance.project.api.entities.PointOfInterest;
import finance.project.api.filters.rules.*;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.*;
import finance.project.api.utils.DurationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
@Slf4j
public class FilterController {

    private static final List<String> TIMEFRAMES = List.of("1min", "3min", "5min", "15min", "30min", "1h", "4h", "daily", "weekly", "monthly");
    private static final List<String> TIMEFRAMESVOLCME = List.of("5min", "15min", "30min", "1h", "4h","daily");

    private final CandleStructureFilter candleStructureFilter;
    private final BenfordLawFilter benfordLawFilter;
    private final BiaisInstitutionalFilter biasInstitutionalFilter;
    private final ContradictorySignalsFilter contradictorySignalsFilter;
    private final CyclesFilter cyclesFilter;
    private final FractalAnalysisFilter fractalAnalysisFilter;
    private final MarketManipulationFilter marketManipulationFilter;
    private final HighTimeframeZoneFilter highTimeframeZoneFilter;
    private final DonchianChannelsFilter donchianChannelsFilter;
    private final LiquidityFilter liquidityFilter;
    private final LowerTimeframeConfluenceFilter lowerTimeframeConfluenceFilter;
    private final ICTPointOfInterestFilter ictPointOfInterestFilter;
    private final MeanReversionProbabilityFilter meanReversionProbabilityFilter;
    private final StatisticalArbitrageFilter statisticalArbitrageFilter;
    private final PsychologicAndNewsFilter psychologicAndNewsFilter;
    private final StationarityFilter stationarityFilter;

    private final CandleService candleService;
    private final MarketDataService marketDataService;
    private final OrderFlowService orderFlowService;
    private final VolatilityFilter volatilityFilter;

    private final SignalRecorderService signalRecorderService;

    private final VolumeBasedRolloverService volumeBasedRolloverService;
    private final SymbolService symbolService;
    private final TA4JService ta4JService;

    private List<CandleDTO> resolveCandles(String symbol, String timeframe, Integer numberLastestCandles,
                                           LocalDateTime startDate, LocalDateTime endDate) {
        if (numberLastestCandles != null && numberLastestCandles > 0) {
            return candleService.getLastCandles(symbol, timeframe, numberLastestCandles);
        } else if (startDate != null && endDate != null) {
            return candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        } else {
            return null;
        }
    }

    private List<Double> resolvePriceVariations(String symbol, String timeframe, Integer numberLastestCandles,
                                                LocalDateTime startDate, LocalDateTime endDate) {
        if (numberLastestCandles != null && numberLastestCandles > 0) {
            return candleService.getPriceVariations(symbol, timeframe, numberLastestCandles);
        } else if (startDate != null && endDate != null) {
            return candleService.getPriceVariations(symbol, timeframe, startDate, endDate);
        } else {
            return null;
        }
    }


    @GetMapping("/bullish-bearish-stats")
    public ResponseEntity<Map<String, String>> getBullishContinuationProbability(@RequestParam String symbol,
                                                                                 @RequestParam String timeframe,
                                                                                 @RequestParam(required = false) Integer numberLastestCandles,
                                                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<CandleDTO> candles = resolveCandles(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (candles == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        Map<String, String> probability = candleStructureFilter.calculateContinuationProbabilities(symbol, timeframe, numberLastestCandles, startDate, endDate);
        return ResponseEntity.ok(probability);
    }

    @GetMapping("/bullish-bearish-stats/multi-timeframes")
    public ResponseEntity<Map<String, Map<String, String>>> getBullishContinuationProbabilityForMultipleTimeframes(
            @RequestParam String symbol,
            @RequestParam String timeframes,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // 1. On split les timeframes
        List<String> requestedTimeframes = Arrays.stream(timeframes.split(","))
                .map(String::trim)
                .map(String::toLowerCase) // ou pas selon ta convention
                .toList();

        // 2. Validation
        List<String> validTimeframes = requestedTimeframes.stream()
                .filter(TIMEFRAMESVOLCME::contains)
                .toList();

        if (validTimeframes.isEmpty()) {
            log.warn("❌ Aucun timeframe valide parmi : {}", requestedTimeframes);
            return ResponseEntity.badRequest().body(Map.of("error", Map.of("message", "Aucun timeframe valide !")));
        }

        // 3. Boucle sur les timeframes et calcul des stats
        for (String tf : validTimeframes) {
            log.info("🔎 Calcul de la probabilité de continuation pour symbol {} sur timeframe {}", symbol, tf);
            List<CandleDTO> candles = resolveCandles(symbol, tf, numberLastestCandles, startDate, endDate);
            if (candles == null || candles.isEmpty()) continue;

            Map<String, String> probability = candleStructureFilter.calculateContinuationProbabilities(symbol, tf, numberLastestCandles, startDate, endDate);
            result.put(tf, probability);
        }

        return ResponseEntity.ok(result);
    }


    @GetMapping("/bullish-bearish-stats/all")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbability(
            @RequestParam String symbol,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMESVOLCME) {
            List<CandleDTO> candles = resolveCandles(symbol, tf, numberLastestCandles, startDate, endDate);
            if (candles == null || candles.isEmpty()) continue;
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf, numberLastestCandles, startDate, endDate));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/bullish-bearish-stats/all/limit")
    public ResponseEntity<Map<String, Map<String, String>>> getAllBullishContinuationProbabilityLimit(
            @RequestParam String symbol,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String tf : TIMEFRAMESVOLCME) {
            List<CandleDTO> candles = resolveCandles(symbol, tf, numberLastestCandles, startDate, endDate);
            if (candles == null || candles.isEmpty()) continue;
            result.put(tf, candleStructureFilter.calculateContinuationProbabilities(symbol, tf, numberLastestCandles, startDate, endDate));
         }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/benford/anomaly")
    public ResponseEntity<?> getBenfordAnomalyScore(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Double> priceChanges;
        if (numberLastestCandles != null && numberLastestCandles > 0) {
            priceChanges = candleService.getPriceVariations(symbol, timeframe, numberLastestCandles);
        } else if (startDate != null && endDate != null) {
            priceChanges = candleService.getPriceVariations(symbol, timeframe, startDate, endDate);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        // Calcul du score de conformité
        double benfordScore = benfordLawFilter.calculateBenfordScore(priceChanges);

        return ResponseEntity.ok(Map.of("benfordScore", benfordScore));
    }

    @GetMapping("/benford/anomaly/save")
    public ResponseEntity<Map<String, Object>> getAndSaveBenfordAnomalyScore(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam List<Integer> horizons) {

        if ((numberLastestCandles == null || numberLastestCandles <= 0) && (startDate == null || endDate == null)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }

        List<CandleDTO> candles;
        List<Double> priceChanges;
        int maxHorizon = Collections.max(horizons);

        if (numberLastestCandles != null && numberLastestCandles > 0) {
            candles = candleService.getLastCandles(symbol, timeframe, numberLastestCandles + maxHorizon);
            priceChanges = candleService.getPriceVariations(symbol, timeframe, numberLastestCandles);
            int baseIndex = numberLastestCandles - 1;
            CandleDTO baseCandle = candles.get(baseIndex);
            double baseClose = baseCandle.getClose().doubleValue();
            LocalDateTime time = baseCandle.getDate();
            for (int h : horizons) {
                CandleDTO futureCandle = candles.get(baseIndex + h);
                double futureClose = futureCandle.getClose().doubleValue();
                signalRecorderService.recordSignal("BenfordAnomaly", symbol, timeframe, time, baseClose, h, futureClose);
            }
        } else {
            Duration tfDuration = DurationUtils.parseTimeframe(timeframe);
            LocalDateTime extendedEnd = endDate.plus(tfDuration.multipliedBy(maxHorizon));
            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, extendedEnd);
            priceChanges = candleService.getPriceVariations(symbol, timeframe, startDate, endDate);
            int baseIndex = candles.size() - maxHorizon - 1;
            CandleDTO baseCandle = candles.get(baseIndex);
            double baseClose = baseCandle.getClose().doubleValue();
            LocalDateTime time = baseCandle.getDate();
            for (int h : horizons) {
                int futureIdx = baseIndex + h;
                if (futureIdx < candles.size()) {
                    double futureClose = candles.get(futureIdx).getClose().doubleValue();
                    signalRecorderService.recordSignal("BenfordAnomaly", symbol, timeframe, time, baseClose, h, futureClose);
                }
            }
        }

        double benfordScore = benfordLawFilter.calculateBenfordScore(priceChanges);
        return ResponseEntity.ok(Map.of("benfordScore", benfordScore));
    }

    @GetMapping("/benford/sliced")
    public ResponseEntity<String> detectBenfordSliced(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam int windowSize,
            @RequestParam List<Integer> horizons) {

        List<CandleDTO> candles;

        if (numberLastestCandles != null && numberLastestCandles > 0) {
            int total = numberLastestCandles + Collections.max(horizons);
            candles = candleService.getLastCandles(symbol, timeframe, total);
        } else if (startDate != null && endDate != null) {
            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        } else {
            return ResponseEntity.badRequest().body("Veuillez spécifier 'numberLastestCandles' ou une plage 'startDate' / 'endDate'.");
        }

        benfordLawFilter.recordBenfordAnomaliesSliced(candles, symbol, timeframe, windowSize, horizons, signalRecorderService);

        return ResponseEntity.ok("Benford anomalies enregistrées par tranches.");
    }

    @GetMapping("/institutional-biais")
    public ResponseEntity<?> getInstitutionalBias(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (candlesLatest == null || candlesLatest.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }

        int bias = biasInstitutionalFilter.calculateInstitutionalBias(candlesLatest);

        Map<String, Integer> result = new HashMap<>();
        result.put("institutionalBias", bias);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/contradictory-signals")
    public ResponseEntity<?> getContradictionScore(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (candlesLatest == null || candlesLatest.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }

        double price = candlesLatest.getFirst().getClose().doubleValue();
        double ema50 = marketDataService.calculateEMA(candlesLatest,50);
        double ema200 = marketDataService.calculateEMA(candlesLatest,200);
        double rsi = marketDataService.calculateRSI(candlesLatest, 14);
        double macd = marketDataService.calculateMACD(candlesLatest, 12, 26);
        double macdSignal = marketDataService.calculateMACDSignal(candlesLatest, 12, 26, 9);
        double stochK = marketDataService.calculateStochasticK(candlesLatest, 14);
        double stochD = marketDataService.calculateStochasticD(candlesLatest, 14, 3);
        double zScore = marketDataService.calculateZScore(candlesLatest, 20);
        double williamsR = marketDataService.calculateWilliamsR(candlesLatest, 14);

        // Calcul du score de contradiction
        int contradictionScore = contradictorySignalsFilter.calculateContradictionScore(
                price, ema50, ema200, rsi, macd, macdSignal, stochK, stochD,williamsR, zScore
        );

        Map<String, Integer> result = new HashMap<>();
        result.put("contradictionScore", contradictionScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/cycles")
    public ResponseEntity<?> getMarketCycles(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Double> prices = resolvePriceVariations(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (prices == null || prices.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }

        // Calcul du coefficient de détermination R²
        double rSquared = cyclesFilter.calculateR2(prices);

        // Détection du cycle dominant (en nombre de bougies)
        int dominantCycle = cyclesFilter.detectDominantCycle(prices);

        Map<String, Double> result = new HashMap<>();
        result.put("rSquared", rSquared);
        result.put("dominantCycle", (double) dominantCycle);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/entropy")
    public ResponseEntity<?> getMarketEntropy(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Double> priceChanges = resolvePriceVariations(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (priceChanges == null || priceChanges.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        // 10000 car EURSUD, à adapter pour le symbole
        double entropy = volatilityFilter.calculateMarketEntropy(priceChanges,10000);

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
    public ResponseEntity<?> getFractalAnalysis(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // Récupération des variations de prix sous forme de rendements
        List<CandleDTO> candles = resolveCandles(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (candles == null || candles.size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            double prev = candles.get(i - 1).getClose().doubleValue();
            double curr = candles.get(i).getClose().doubleValue();
            returns.add((curr - prev) / prev);
        }

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
    public ResponseEntity<?> detectMarketManipulation(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Double> priceChanges = resolvePriceVariations(symbol, timeframe, numberLastestCandles, startDate, endDate);
        if (priceChanges == null || priceChanges.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        int manipulationScore = marketManipulationFilter.detectManipulationZone(priceChanges);

        Map<String, Integer> result = new HashMap<>();
        result.put("manipulationScore", manipulationScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-timeframe-zones")
    public ResponseEntity<?> getHighTimeframeZones(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<CandleDTO> candles = resolveCandles(symbol, "5min", numberLastestCandles != null ? numberLastestCandles : 1, startDate, endDate);
        if (candles == null || candles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        double price = candles.get(candles.size()-1).getClose().doubleValue();
        List<PointOfInterest> keyLevels = candleService.getInstitutionalLevels(symbol,timeframe);

        // Calcul du score de confluence
        int confluenceScore = highTimeframeZoneFilter.checkInstitutionalConfluence(price, keyLevels);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/high-timeframe-zones-with-orderflow")
    public ResponseEntity<?> getHighTimeframeZonesWithOrderFlow(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<CandleDTO> candles = resolveCandles(symbol, timeframe, numberLastestCandles != null ? numberLastestCandles : 1, startDate, endDate);
        if (candles == null || candles.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate'."));
        }
        double price = candles.get(candles.size()-1).getClose().doubleValue();
        List<PointOfInterest> keyLevels = candleService.getInstitutionalLevels(symbol,timeframe);

        List<Double> buyVolumes;
        List<Double> sellVolumes;
        if (startDate != null && endDate != null) {
            buyVolumes = orderFlowService.getBuyVolumes(symbol, keyLevels, startDate, endDate);
            sellVolumes = orderFlowService.getSellVolumes(symbol, keyLevels, startDate, endDate);
        } else {
            LocalDateTime now = LocalDateTime.now();
            buyVolumes = orderFlowService.getBuyVolumes(symbol, keyLevels, now.minusHours(1), now);
            sellVolumes = orderFlowService.getSellVolumes(symbol, keyLevels, now.minusHours(1), now);
        }

        // Calcul du score final
        int confluenceScore = highTimeframeZoneFilter.checkInstitutionalConfluenceWithOrderFlow(price, keyLevels, buyVolumes, sellVolumes);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/donchian-channels")
    public ResponseEntity<?> getDonchianChannels(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        int candlesToFetch = numberLastestCandles != null ? numberLastestCandles : period;
        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, candlesToFetch, startDate, endDate);
        if (candlesLatest == null || candlesLatest.size() < period) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate' avec assez de données."));
        }
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
    public ResponseEntity<?> getMarketLiquidity(
            @RequestParam String symbol, @RequestParam String timeframe,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // Extraction des closes, highs et lows
        int candlesToFetch = numberLastestCandles != null ? numberLastestCandles : period;
        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, candlesToFetch, startDate, endDate);
        if (candlesLatest == null || candlesLatest.size() < period) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate' avec assez de données."));
        }
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
    public ResponseEntity<?> getLowerTimeframeConfluence(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam int period,
            @RequestParam(required = false) Integer numberLastestCandles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        int candlesToFetch = numberLastestCandles != null ? numberLastestCandles : period;
        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, candlesToFetch, startDate, endDate);
        if (candlesLatest == null || candlesLatest.size() < period) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Veuillez fournir soit 'numberLastestCandles' soit 'startDate'/'endDate' avec assez de données."));
        }

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
        double deltaVolume;
        if (startDate != null && endDate != null) {
            deltaVolume = orderFlowService.getDeltaVolume(symbol, timeframe, startDate, endDate);
        } else {
            LocalDateTime now = LocalDateTime.now();
            deltaVolume = orderFlowService.getDeltaVolume(symbol, timeframe, now.minusHours(1), now);
        }

        // Calcul du score de confluence
        int confluenceScore = lowerTimeframeConfluenceFilter.calculateConfluenceScore(momentum, adx, trendAligned, vwapDistance, deltaVolume);

        Map<String, Integer> result = new HashMap<>();
        result.put("confluenceScore", confluenceScore);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/analyzeICTpoi-multitf")
    public ResponseEntity<List<PointOfInterest>> analyzeICTMultiTf(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            List<CandleDTO> m15 = candleService.getCandlesByTimeframeAndIntervalDate(symbol, "15min", startDate, endDate);
            List<CandleDTO> h1 = candleService.getCandlesByTimeframeAndIntervalDate(symbol, "1h", startDate, endDate);
            List<CandleDTO> daily = candleService.getCandlesByTimeframeAndIntervalDate(symbol, "daily", startDate.minusDays(7), endDate);
            List<CandleDTO> weekly = candleService.getCandlesByTimeframeAndIntervalDate(symbol, "weekly", startDate.minusWeeks(4), endDate);
            List<CandleDTO> monthly = candleService.getCandlesByTimeframeAndIntervalDate(symbol, "monthly", startDate.minusMonths(6), endDate);

            List<PointOfInterest> points = new ArrayList<>();

            // ✅ News Gaps : uniquement D / W
            points.addAll(ictPointOfInterestFilter.detectNewsOpenGaps(daily));
            points.addAll(ictPointOfInterestFilter.detectNewsOpenGaps(weekly));

            // ✅ Previous High/Low : uniquement D / W / M
            points.addAll(ictPointOfInterestFilter.detectPreviousHighsLows(daily));
            points.addAll(ictPointOfInterestFilter.detectPreviousHighsLows(weekly));
            points.addAll(ictPointOfInterestFilter.detectPreviousHighsLows(monthly));

            // ✅ Intraday Patterns
            points.addAll(ictPointOfInterestFilter.detectFairValueGaps(m15));
            points.addAll(ictPointOfInterestFilter.detectOrderBlocks(m15));
            points.addAll(ictPointOfInterestFilter.detectFibonacciRetracements(h1));

            // ✅ Global Levels (si tu veux les garder génériques)
            points.addAll(ictPointOfInterestFilter.detectPsychologicalLevels(m15));
            points.addAll(ictPointOfInterestFilter.detectVolumeProfileLevels(m15));

            points.addAll(ictPointOfInterestFilter.detectBreakawayGaps(m15));
            points.addAll(ictPointOfInterestFilter.detectContinuationGaps(m15));

            return ResponseEntity.ok(points);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }

    /**
     *
     * @param symbol
     * @param timeframe
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/volatility")
    public Map<String, Double> getVolatilityAnalysis(@RequestParam String symbol, @RequestParam String timeframe,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }

        return volatilityFilter.analyzeVolatility(ta4JService.convertToTimeSeries(candles,timeframe));
    }

    @GetMapping("/sharpe-ratio")
    public double getSharpeRatio(@RequestParam String symbol,
                                 @RequestParam String timeframe,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }
        return marketDataService.calculateSharpeRatio(candles);
    }

    @GetMapping("/sortino-ratio")
    public double getSortinoRatio(@RequestParam String symbol,
                                  @RequestParam String timeframe,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }
        return marketDataService.calculateSortinoRatio(candles);
    }

    @GetMapping("/mean-reversion-signal")
    public boolean getMeanReversionSignal(@RequestParam String symbol,
                                          @RequestParam String timeframe,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                          @RequestParam int period,
                                          @RequestParam(required = false) Integer numberLastestCandles) {
        int candlesToFetch = numberLastestCandles != null ? numberLastestCandles : period;
        List<CandleDTO> candlesLatest = resolveCandles(symbol, timeframe, candlesToFetch, startDate, endDate);
        if (candlesLatest == null) {
            return false;
        }
        return meanReversionProbabilityFilter.isMeanReversionSignal(candlesLatest, period, timeframe);
    }

    @GetMapping("/stationarity-signal")
    public boolean getStationaritySignal(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam int period) {

        List<CandleDTO> candles;
        if ("1min".equals(timeframe)) {
            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);
        } else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);
            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }

        List<Double> closePrices = candles.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());

        double[] closePricesArray = closePrices.stream().mapToDouble(Double::doubleValue).toArray();

        double adfStatistic = stationarityFilter.test(closePricesArray,1);

        double criticalValue = -3.45; // Valeur critique à 5% pour un grand échantillon
        return adfStatistic < criticalValue;
    }

    @GetMapping("/statistic-arbitrage")
    public ResponseEntity<Map<String, Double>> calculateArbitrageMetrics(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam double omegaThreshold,
            @RequestParam double benchmarkReturn) {

        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }
        List<Double> closePrices = candles.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());

        // Calculer les rendements quotidiens
        List<Double> dailyReturns = statisticalArbitrageFilter.calculateDailyReturns(closePrices);

        // Calculer les ratios
        double omegaRatio = statisticalArbitrageFilter.calculateOmegaRatio(dailyReturns, omegaThreshold);
        double informationRatio = statisticalArbitrageFilter.calculateInformationRatio(dailyReturns, benchmarkReturn);

        Map<String, Double> result = new HashMap<>();
        result.put("omegaRatio", omegaRatio);
        result.put("informationRatio", informationRatio);
        // Retourner les métriques calculées
        return ResponseEntity.ok(result);
    }

    @GetMapping("/drawdown")
    public double getDrawdown(
            @RequestParam double currentClose,
            @RequestParam double highestClose) {
        return psychologicAndNewsFilter.calculatePercentageDrawdown(currentClose, highestClose);
    }

    // 🔹 Endpoint pour calculer l'Ulcer Index sur une période donnée
    @GetMapping("/ulcer-index")
    public double getUlcerIndex(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam int period) {

        List<CandleDTO> candles;
        if(timeframe.equals("1min")){

            candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeOld(startDate, endDate, 2);

        }
        else {
            SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
            System.out.println(symbolDTO);

            candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);
        }
        List<Double> closePrices = candles.stream()
                .map(candle -> candle.getClose().doubleValue())
                .collect(Collectors.toList());

        return psychologicAndNewsFilter.calculateUlcerIndex(closePrices, period);
    }
}
