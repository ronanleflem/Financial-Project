package finance.project.api.controllers;



import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.entities.TradeCompleted;
import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.CandleFilterDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.model.TradeCompletedDTO;
import finance.project.api.services.*;
import org.apache.commons.lang3.tuple.Pair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le contrôleur {@code CandleController} est un contrôleur REST qui gère les requêtes HTTP liées aux bougies (candles) financières.
 * Il expose des endpoints pour récupérer des données de bougies en fonction des symboles et des intervalles spécifiés.
 * <p>
 * Cette classe est annotée avec {@link RestController}, ce qui la rend apte à gérer les requêtes HTTP et à renvoyer des réponses JSON.
 * L'annotation {@link RequestMapping} spécifie que toutes les requêtes à ce contrôleur seront préfixées par "/api/finance/charts".
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/charts")
public class CandleController {

    /**
     * Service pour la gestion des bougies.
     */
    private final CandleService candleService;

    private final MarketstackService marketstackService;

    private final CurrencyLayerService currencyLayerService;

    private final CandleAggregationService candleAggregationService;

    private final VolumeBasedRolloverService volumeBasedRolloverService;

    private final TradeCompletedService tradeCompletedService;
    private final TradeCompletedMapper tradeCompletedMapper;
    private final DeltaLakeCandleReader deltaLakeCandleReader;

    private final BinanceService binanceService;

    /**
     * Service pour la gestion des symboles.
     */
    private final SymbolService symbolService;

    private static final List<String> TIMEFRAMES = List.of("1min", "3min", "5min", "15min", "30min", "1h", "4h", "daily", "weekly", "monthly");
    private static final List<String> TIMEFRAMESVOL = List.of("1min", "3min", "5min", "10min", "15min", "30min", "1h", "2h","4h","8h", "12h", "daily", "weekly", "monthly");
    private static final List<String> TIMEFRAMESVOLCME = List.of("3min", "5min", "10min", "15min", "30min", "1h", "2h","4h","8h", "12h", "daily", "weekly", "monthly");
    private static final List<String> TIMEFRAMESVOLCRYPTO = List.of("1min","3min", "5min", "10min", "15min", "30min", "1h", "2h","4h","8h", "12h", "daily", "weekly", "monthly");


    /**
     * Récupère une liste de bougies (candles) en fonction du symbole et de l'intervalle fournis.
     * <p>
     * Cette méthode répond aux requêtes GET à l'URL "/api/finance/charts" avec les paramètres de requête "symbol" et "interval".
     * Elle utilise le service {@link CandleService} pour obtenir les données et renvoie une réponse HTTP avec le statut 200 OK et les données.
     * </p>
     *
     * @param symbol le symbole de négociation pour lequel les bougies doivent être récupérées (ex: "AAPL")
     * @param timeframe l'intervalle de temps pour les bougies (ex: "daily", "hourly")
     * @return une réponse HTTP contenant la liste des bougies correspondant au symbole et à l'intervalle spécifiés
     */
    @GetMapping("/candles")
    public ResponseEntity<List<CandleDTO>> getCandles(@RequestParam String symbol, @RequestParam String timeframe) {

        SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
        System.out.println(symbolDTO);
        List<CandleDTO> data = candleService.getLastCandles(symbol,timeframe,50);
        System.out.println("Data sended : "+data.size());
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    /** FIXME : Ajouter le symbol dans getDynamicRollover
     *
     * @param symbol
     * @param timeframe
     * @param startDate
     * @param endDate
     * @param analysisPeriodDays
     * @return
     */
    @GetMapping("/candles/date-time")
    public ResponseEntity<List<CandleDTO>> getCandlesDateTime(@RequestParam String symbol, @RequestParam String timeframe,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate, @RequestParam(defaultValue = "2") int analysisPeriodDays) {

        if (!startDate.isBefore(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }

        SymbolDTO symbolDTO = symbolService.getSymbolByCode(symbol);
        System.out.println(symbolDTO);

        List<CandleDTO> candles = candleService.getCandlesByTimeframeAndIntervalDate(symbol, timeframe, startDate, endDate);

        System.out.println("Number candles sended : "+candles.size());
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }

    /**
     * Récupère une liste par défaut de bougies pour le symbole "AAPL" avec l'intervalle "daily".
     * <p>
     * Cette méthode répond aux requêtes GET à l'URL "/api/finance/charts/default".
     * Elle utilise le service {@link CandleService} pour obtenir les données par défaut et renvoie directement les bougies sans spécifier de symbole ni d'intervalle dans la requête.
     * </p>
     *
     * @return la liste des bougies pour le symbole "AAPL" avec l'intervalle "daily"
     */
    @GetMapping("/default")
    public List<CandleDTO> listCandles(){

        SymbolDTO symbol = symbolService.getSymbolByCode("AAPL");

        return candleService.getCandles(symbol, "daily");
    }


    @GetMapping("/yahoo")
    public ResponseEntity<List<CandleDTO>> getYahooCandles(@RequestParam String symbol) {
        List<CandleDTO> candles = candleService.getCandles(symbol);
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }

    @GetMapping("/load-csv/tradingview")
    public ResponseEntity<List<CandleDTO>> loadTradingViewCsv(@RequestParam String symbol, @RequestParam String timeframe) {
        List<CandleDTO> candles = candleService.loadCsvTradingView(symbol,timeframe,false);
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }

    @GetMapping("/load-csv/tradingview/all")
    public ResponseEntity<List<CandleDTO>> loadAllTradingViewCsv(@RequestParam String symbol) {
        for(String timeframe : TIMEFRAMES){
            candleService.loadCsvTradingView(symbol, timeframe,false);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/load-csv/tradingview-with-volume/all")
    public ResponseEntity<List<CandleDTO>> loadAllTradingViewCsvWithVolume(@RequestParam String symbol) {
        for(String timeframe : TIMEFRAMESVOL){
            candleService.loadCsvTradingView(symbol, timeframe,true);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     *  Candle filtrée - à utiliser pour vérifier l'efficacité d'une stratégie selon un condition spécifique
     *      (killzone - jours spécifique - mois spécifique - Semestre / Trimestre - année spécifique)
     * @param session
     * @param marketCondition
     * @param newsEvent
     * @param startDate
     * @param endDate
     * @return
     */
    @GetMapping("/candlesFiltered")
    public ResponseEntity<List<Candle>> getFilteredCandles(
            @RequestParam(required = false) String session,
            @RequestParam(required = false) String marketCondition,
            @RequestParam(required = false) String newsEvent,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        CandleFilterDTO filter = new CandleFilterDTO();
        filter.setSession(session);
        filter.setMarketCondition(marketCondition);
        filter.setNewsEvent(newsEvent);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        List<Candle> candles = candleService.getFilteredCandles(filter);
        return ResponseEntity.ok(candles);
    }

    @GetMapping("/marketstack/historical")
    public ResponseEntity<List<Map<String, Object>>> getHistoricalData() {
        List<Map<String, Object>> data = marketstackService.getHistoricalEURUSD();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/currencyLayer/live")
    public ResponseEntity<Map<String, Double>> getLiveExchangeRate() {
        double rate = currencyLayerService.getLiveExchangeRate();
        Map<String, Double> result = new HashMap<>();
        result.put("EUR/USD", rate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/currencyLayer/historical")
    public ResponseEntity<Map<String, Double>> getHistoricalExchangeRate(
            @RequestParam String date) {

        double rate = currencyLayerService.getHistoricalExchangeRate(date);
        Map<String, Double> result = new HashMap<>();
        result.put("EUR/USD (" + date + ")", rate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/load-csv/cme")
    public ResponseEntity<List<CandleDTO>> loadCmeCsv(
            @RequestParam String symbol, @RequestParam String timeframe, @RequestParam String data) {

        List<CandleDTO> candles = candleService.loadCsvCME(symbol, timeframe, data);
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }
    private Pair<LocalDateTime, LocalDateTime> getStartAndEndDateFromData(String data) {
        try {
            // Exemple data : "data_2025-02"
            String[] split = data.split("_");
            if (split.length < 2) {
                throw new IllegalArgumentException("Format du paramètre 'data' incorrect. Ex: data_YYYY-MM");
            }

            String yearMonthStr = split[1];
            YearMonth yearMonth = YearMonth.parse(yearMonthStr);

            LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(23, 59, 59);

            return Pair.of(startDateTime, endDateTime);

        } catch (Exception e) {
            throw new RuntimeException("❌ Erreur lors du parsing de la période depuis 'data' : " + data, e);
        }
    }

    @GetMapping("/load-csv/cme/all-timeframes")
    public ResponseEntity<List<CandleDTO>> loadCmeCsvAllTimeframe(
            @RequestParam String symbol,
            @RequestParam String timeframe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "2") int analysisPeriodDays,
            @RequestParam String data
    ) {
        // Si pas de startDate / endDate -> on utilise data_YYYY-MM pour déduire
        if (startDate == null || endDate == null) {
            var dates = getStartAndEndDateFromData(data);
            startDate = dates.getLeft();
            endDate = dates.getRight();
        }

        // Charger les candles du fichier CSV spécifique
        List<CandleDTO> candles = candleService.loadCsvCME(symbol, timeframe, data);

        // Agrégation sur toutes les timeframes que tu as défini
        for (String e : TIMEFRAMESVOLCME) {
            List<CandleDTO> aggregatedCandles = candleAggregationService.aggregateCandles(candles, e, MarketType.CME);
            candleService.saveCandlesToDatabase(aggregatedCandles, symbol, e);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/from-trade")
    public Map<String, Object> getCandlesForTrade(
            @RequestParam Long tradeId,
            @RequestParam(defaultValue = "5min") String timeframe,
            @RequestParam String symbol,
            @RequestParam String comparedSymbol,
            @RequestParam(defaultValue = "0") int beforeCandles,
            @RequestParam(defaultValue = "0") int afterCandles) {

        TradeCompleted trade = tradeCompletedService.getTradeById(tradeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trade not found"));
        TradeCompletedDTO tradeDto = tradeCompletedMapper.toDto(trade);
        String tradeSymbol = trade.getSymbol();

        // A ajouter trade.getSymbol() au lieu de EURUSD en dur
        List<CandleDTO> candles = candleService.getCandlesForTrade(trade, tradeSymbol, timeframe, beforeCandles, afterCandles);
        if (candles.isEmpty()) {
            candles = deltaLakeCandleReader.getCandlesForTrade(trade, timeframe, beforeCandles, afterCandles);
        }
        List<CandleDTO> candlesComparedSymbol = candleService.getCandlesForTrade(trade, comparedSymbol, timeframe, beforeCandles, afterCandles);

        Map<String, Object> response = new HashMap<>();
        response.put("candles", candles);
        response.put("comparedCandles", candlesComparedSymbol);
        response.put("trade", tradeDto);

        return response;
    }

    @GetMapping("/binance/historical")
    public ResponseEntity<List<CandleDTO>> loadBinanceHistorical(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(defaultValue = "500") int limit) {

        List<CandleDTO> candles = binanceService.getHistoricalCandles(symbol, interval, limit);
        candleService.saveCandlesToDatabase(candles, symbol, interval);
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }

    @GetMapping("/binance/historical-range")
    public ResponseEntity<List<CandleDTO>> loadBinanceHistoricalRange(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<CandleDTO> candles = binanceService.getHistoricalCandlesInRange(symbol, interval, startDate, endDate);
        // Agrégation sur toutes les timeframes que tu as défini
        for (String e : TIMEFRAMESVOLCRYPTO) {
            List<CandleDTO> aggregatedCandles = candleAggregationService.aggregateCandles(candles, e, MarketType.CRYPTO);
            candleService.saveCandlesToDatabase(aggregatedCandles, symbol, e);
        }
        return new ResponseEntity<>(candles, HttpStatus.OK);
    }

}
