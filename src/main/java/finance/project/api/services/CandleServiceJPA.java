package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    @Transactional
    public List<CandleDTO> getCandles(String symbol) {
        Symbol existingSymbol = symbolRepository.findBySymbol(symbol).orElseThrow();

        if (existingSymbol == null) {
            throw new IllegalArgumentException("❌ Symbole non trouvé en base : " + symbol);
        }

        // Vérifier si on a déjà des données en base
        List<Candle> candlesFromDB = candleRepository.findBySymbol(existingSymbol);
        if (!candlesFromDB.isEmpty()) {
            log.info("📊 Retour des données depuis la base pour {}", symbol);
            return candlesFromDB.stream().map(this::mapToDTO).toList();
        }

        // Sinon, récupérer depuis Yahoo et stocker en base
        log.info("🌍 Récupération des données Yahoo Finance pour {}", symbol);
        List<CandleDTO> candlesFromYahoo = alphaVantageService.getHistoricalData(symbol);
        if (!Objects.requireNonNull(candlesFromYahoo).isEmpty()) {
            saveCandlesToDatabase(Objects.requireNonNull(candlesFromYahoo), existingSymbol);
        }

        return candlesFromYahoo;
    }

    @Transactional
    public void saveCandlesToDatabase(List<CandleDTO> candles, Symbol symbol) {
        List<Candle> candleEntities = candles.stream().map(dto -> Candle.builder()
                .symbol(symbol)
                .date(dto.getDate())
                .open(dto.getOpen())
                .close(dto.getClose())
                .high(dto.getHigh())
                .low(dto.getLow())
                .volume(dto.getVolume())
                .build()
        ).toList();

        candleRepository.saveAll(candleEntities);
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