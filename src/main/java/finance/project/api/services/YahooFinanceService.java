package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YahooFinanceService {

    public List<CandleDTO> getHistoricalData(String symbol) {
        List<CandleDTO> candles = new ArrayList<>();

        try {
            Stock stock = YahooFinance.get(symbol, true);
            if (stock == null) {
                log.error("❌ Impossible de récupérer les données pour {}", symbol);
                return candles;
            }

            // Définition de la période à récupérer (ex: 1 an d'historique)
            Calendar from = Calendar.getInstance();
            from.add(Calendar.YEAR, -1);

            // Transformation des données Yahoo en CandleDTO
            candles = stock.getHistory(from).stream()
                    .map(hist -> CandleDTO.builder()
                            .date(hist.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                            .open(hist.getOpen())
                            .close(hist.getClose())
                            .high(hist.getHigh())
                            .low(hist.getLow())
                            .volume(BigDecimal.valueOf(hist.getVolume()))
                            .build()
                    ).collect(Collectors.toList());

            log.info("✅ {} bougies récupérées pour {}", candles.size(), symbol);

        } catch (IOException e) {
            log.error("❌ Erreur lors de la récupération des données Yahoo Finance", e);
        }

        return candles;
    }
}
