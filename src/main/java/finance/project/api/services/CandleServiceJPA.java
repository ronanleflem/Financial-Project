package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.CandleFilterDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.utils.CandleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CandleServiceJPA implements CandleService {

    private final YahooFinanceService yahooFinanceService;
    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final AlphaVantageService alphaVantageService;



    @Override
    public List<CandleDTO> getCandles(SymbolDTO symbol, String interval) {
        return List.of();
    }

    @Override
    public List<CandleDTO> getLastCandles(String symbol, String timeframe, int limit){
        Optional<Symbol> existingSymbol = symbolRepository.findBySymbol(symbol);
        List<Candle> candlesFromDB = candleRepository.findBySymbolAndTimeframeOrderByDateAscLimitNumberLatestCandle(existingSymbol,  timeframe, limit);
        if (!candlesFromDB.isEmpty()) {
            log.info("📊 Retour des données depuis la base pour {}", symbol);
            return candlesFromDB.stream().map(this::mapToDTO).toList();
        }
        return List.of();
    }

    @Override
    public List<CandleDTO> getCandles(String symbol) {
        Symbol existingSymbol = symbolRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("❌ Symbole non trouvé en base : " + symbol));

        // Vérifier si on a déjà des données en base
        List<Candle> candlesFromDB = candleRepository.findBySymbol(existingSymbol);
        if (!candlesFromDB.isEmpty()) {
            log.info("📊 Retour des données depuis la base pour {}", symbol);
            return candlesFromDB.stream().map(this::mapToDTO).toList();
        }
        // Récupération des données Yahoo
        log.info("🌍 Récupération des données Yahoo Finance pour {}", symbol);
        List<CandleDTO> candlesFromYahoo = alphaVantageService.getHistoricalData(symbol);
        if (candlesFromYahoo != null && !candlesFromYahoo.isEmpty()) {
            saveCandlesToDatabase(candlesFromYahoo, existingSymbol,"Daily");
        }
        return candlesFromYahoo;
    }

    @Override
    public List<Double> getPriceVariations(String symbol, String timeframe, int limit) {
        List<Candle> candles = candleRepository.findBySymbolAndTimeframeOrderByDateAscLimitNumberLatestCandle(symbolRepository.findBySymbol(symbol), timeframe, limit);

        List<Double> priceVariations = new ArrayList<>();
        for (Candle candle : candles) {
            double variation = Math.abs(candle.getClose().subtract(candle.getOpen()).doubleValue());
            if (variation > 0) { // Éviter les 0
                priceVariations.add(variation);
            }
        }
        return priceVariations;
    }

    @Override
    public List<CandleDTO> loadCsvTradingView(String symbolName, String timeframe, Boolean volume) {
        String filePath;
        // Charger le symbole depuis la base
        Symbol symbol = symbolRepository.findBySymbol(symbolName)
                .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolName));
        if(volume) {
            filePath = "csvData/" + symbolName.toLowerCase() + "/" + timeframe + "/" + timeframe + "Vol.csv";
        }
        else{
            filePath = "csvData/" + symbolName.toLowerCase() + "/"+timeframe+"/"+timeframe+".csv";
        }
        List<CandleDTO> candles = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            CandleDTO candleDTO;
            while ((line = br.readLine()) != null) {
                // Ignorer la première ligne si elle contient du texte
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("time")) continue;
                }
                String[] values = line.split(",");
                if (values.length < 5) continue;
                if(!volume){
                    candleDTO = CandleDTO.builder()
                            .date(Instant.ofEpochSecond(Long.parseLong(values[0])).atZone(ZoneId.of("UTC")).toLocalDateTime())
                            .open(new BigDecimal(values[1]))
                            .high(new BigDecimal(values[2]))
                            .timeframe(timeframe)
                            .low(new BigDecimal(values[3]))
                            .close(new BigDecimal(values[4]))
                            .symbol(SymbolDTO.builder().id(symbol.getId()).name(symbol.getName()).build())
                            .build();
                }
                else {
                    candleDTO = CandleDTO.builder()
                            .date(Instant.ofEpochSecond(Long.parseLong(values[0])).atZone(ZoneId.of("UTC")).toLocalDateTime())
                            .open(new BigDecimal(values[1]))
                            .high(new BigDecimal(values[2]))
                            .timeframe(timeframe)
                            .low(new BigDecimal(values[3]))
                            .close(new BigDecimal(values[4]))
                            .volume(new BigDecimal(values[5]))
                            //.volumeAverage(new BigDecimal(values[6]))
                            .volumeAverage(null)
                            .openInterest(new BigDecimal(values[14]))
                            .symbol(SymbolDTO.builder().id(symbol.getId()).name(symbol.getName()).build())
                            .build();
                }
                candles.add(candleDTO);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + filePath, e);
        }

        // Sauvegarde en base
        saveCandlesToDatabase(candles, symbol, timeframe);

        return candles;
    }

    public void saveCandlesToDatabase(List<CandleDTO> candles, Symbol symbol, String timeframe) {
        int batchSize = 50;  // On insère 50 bougies à la fois
        List<Candle> batch = new ArrayList<>();

        for (CandleDTO dto : candles) {
            batch.add(Candle.builder()
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .date(dto.getDate())
                    .open(dto.getOpen())
                    .close(dto.getClose())
                    .high(dto.getHigh())
                    .low(dto.getLow())
                    .volume(dto.getVolume())
                    .build());

            if (batch.size() >= batchSize) {
                candleRepository.saveAll(batch);
                batch.clear(); // On vide la liste pour le prochain batch
            }
        }

        if (!batch.isEmpty()) { // Sauvegarde du reste des données
            candleRepository.saveAll(batch);
        }

        log.info("💾 {} bougies enregistrées pour {}", candles.size(), symbol.getSymbol());
    }

    private CandleDTO mapToDTO(Candle candle) {
        return CandleDTO.builder()
                .id(candle.getId())
                .symbol(SymbolDTO.builder()
                        .id(candle.getSymbol().getId())
                        .symbol(candle.getSymbol().getSymbol())
                        .name(candle.getSymbol().getName())
                        .market(candle.getSymbol().getMarket())
                        .build())
                .date(candle.getDate())
                .open(candle.getOpen())
                .close(candle.getClose())
                .high(candle.getHigh())
                .low(candle.getLow())
                .volume(candle.getVolume())
                .build();
    }

    /**
     * Récupère les niveaux institutionnels à partir des timeframes élevés (Daily, Weekly, Monthly).
     *
     * @param symbol Actif à analyser
     * @return Liste des niveaux institutionnels pertinents
     */
    public List<Double> getInstitutionalLevels(String symbol) {
        List<CandleDTO> dailyCandles = candleRepository.findLastNCandles(symbol, "D", 10);
        List<CandleDTO> weeklyCandles = candleRepository.findLastNCandles(symbol, "W", 10);
        List<CandleDTO> monthlyCandles = candleRepository.findLastNCandles(symbol, "M", 10);

        // 1️⃣ Previous Highs & Lows (Daily, Weekly, Monthly)
        double prevDailyHigh = dailyCandles.get(dailyCandles.size() - 2).getHigh().doubleValue();
        double prevDailyLow = dailyCandles.get(dailyCandles.size() - 2).getLow().doubleValue();
        double prevWeeklyHigh = weeklyCandles.get(weeklyCandles.size() - 2).getHigh().doubleValue();
        double prevWeeklyLow = weeklyCandles.get(weeklyCandles.size() - 2).getLow().doubleValue();
        double prevMonthlyHigh = monthlyCandles.get(monthlyCandles.size() - 2).getHigh().doubleValue();
        double prevMonthlyLow = monthlyCandles.get(monthlyCandles.size() - 2).getLow().doubleValue();

        // 2️⃣ New Daily / Weekly Open Gaps
        double dailyOpenGap = Math.abs(dailyCandles.get(dailyCandles.size() - 1).getOpen().doubleValue()
                - dailyCandles.get(dailyCandles.size() - 2).getClose().doubleValue());
        double weeklyOpenGap = Math.abs(weeklyCandles.get(weeklyCandles.size() - 1).getOpen().doubleValue()
                - weeklyCandles.get(weeklyCandles.size() - 2).getClose().doubleValue());

        // 3️⃣ Fair Value Gaps (FVG)
        double fvgDaily = Math.abs(dailyCandles.get(dailyCandles.size() - 3).getHigh().doubleValue()
                - dailyCandles.get(dailyCandles.size() - 1).getLow().doubleValue());
        double fvgWeekly = Math.abs(weeklyCandles.get(weeklyCandles.size() - 3).getHigh().doubleValue()
                - weeklyCandles.get(weeklyCandles.size() - 1).getLow().doubleValue());

        // 4️⃣ Inverted Fair Value Gaps (IFVG)
        double ifvgDaily = Math.abs(dailyCandles.get(dailyCandles.size() - 3).getLow().doubleValue()
                - dailyCandles.get(dailyCandles.size() - 1).getHigh().doubleValue());

        // 5️⃣ Order Blocks (OB)
        double orderBlockDaily = dailyCandles.get(dailyCandles.size() - 3).getOpen().doubleValue();
        double orderBlockWeekly = weeklyCandles.get(weeklyCandles.size() - 3).getOpen().doubleValue();

        // 6️⃣ Psychological Levels (ex: 1.1000, 1.2000 pour EUR/USD)
        double roundNumber1 = Math.round(prevDailyHigh * 10) / 10.0;
        double roundNumber2 = Math.round(prevDailyLow * 10) / 10.0;

        return List.of(
                prevDailyHigh, prevDailyLow, prevWeeklyHigh, prevWeeklyLow, prevMonthlyHigh, prevMonthlyLow,
                dailyOpenGap, weeklyOpenGap, fvgDaily, fvgWeekly, ifvgDaily,
                orderBlockDaily, orderBlockWeekly, roundNumber1, roundNumber2
        );
    }

    /**
     * Récupère le prix actuel.
     */
    public double getCurrentPrice(String symbol) {
        return candleRepository.findLatestPrice(symbol);
    }

    /**
     * Calcule les retours (Price Returns) à partir des prix de clôture.
     *
     * @param symbol Actif à analyser
     * @param timeframe Unité de temps (M1, M5, H1, etc.)
     * @param period Nombre de bougies à récupérer
     * @return Liste des rendements successifs
     */
    public List<Double> getPriceReturns(String symbol, String timeframe, int period) {
        Optional<Symbol> symbolOp = symbolRepository.findBySymbol(symbol);
        List<Candle> candles = candleRepository.findBySymbolAndTimeframeOrderByDateAscLimitNumberLatestCandle(symbolOp, timeframe, period);

        if (candles.size() < 2) {
            throw new IllegalArgumentException("Pas assez de données pour calculer les retours.");
        }

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            double closePrev = candles.get(i - 1).getClose().doubleValue();
            double closeCurrent = candles.get(i).getClose().doubleValue();
            double priceReturn = (closeCurrent - closePrev) / closePrev;
            returns.add(priceReturn);
        }

        return returns;
    }

    /**
     * Récupère les données des Candle en fonction du filtre donné
     *  - Market condition
     *  - News events
     *  - Session
     *  - Start date et end date (à améliorer surement pour éviter de taper trop de fois dans la BDD
     *
     *
     * @param filter
     * @return
     */
    public List<Candle> getFilteredCandles(CandleFilterDTO filter) {
        return candleRepository.findAll(new CandleSpecification(filter));
    }

}