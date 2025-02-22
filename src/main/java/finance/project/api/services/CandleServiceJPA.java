package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
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
    public List<CandleDTO> loadCsvTradingView(String symbolName, String timeframe) {
        // Charger le symbole depuis la base
        Symbol symbol = symbolRepository.findBySymbol(symbolName)
                .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolName));

        String filePath = "csvData/" + symbolName.toLowerCase() + "/"+timeframe+"/"+timeframe+".csv";
        List<CandleDTO> candles = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                // Ignorer la première ligne si elle contient du texte
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("time")) continue;
                }
                String[] values = line.split(",");
                if (values.length < 5) continue;

                CandleDTO candleDTO = CandleDTO.builder()
                        .date(Instant.ofEpochSecond(Long.parseLong(values[0])).atZone(ZoneId.of("UTC")).toLocalDateTime())
                        .open(new BigDecimal(values[1]))
                        .high(new BigDecimal(values[2]))
                        .timeframe(timeframe)
                        .low(new BigDecimal(values[3]))
                        .close(new BigDecimal(values[4]))
                        .symbol(SymbolDTO.builder().id(symbol.getId()).name(symbol.getName()).build())
                        .build();

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

}