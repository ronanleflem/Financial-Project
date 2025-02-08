package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlphaVantageService {

    private static final String API_KEY = "4NBH950DAGIXA6LV";
    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final WebClient webClient;

    public AlphaVantageService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<CandleDTO> getHistoricalData(String symbol) {
        List<CandleDTO> candles = new ArrayList<>();

        try {
            // Appel à l'API via WebClient, réponse en Mono
            Mono<Map<String, Object>> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});

            // Attente du résultat et traitement
            Map<String, Object> response = responseMono.block();  // block() pour synchroniser

            if (response == null || !response.containsKey("Time Series (Daily)")) {
                log.error("❌ Impossible de récupérer les données pour {}", symbol);
                return candles;
            }

            // Extraction des données des bougies
            Map<String, Map<String, String>> timeSeries = (Map<String, Map<String, String>>) response.get("Time Series (Daily)");

            candles = timeSeries.entrySet().stream()
                    .map(entry -> {
                        LocalDate date = LocalDate.parse(entry.getKey());
                        Map<String, String> values = entry.getValue();
                        return CandleDTO.builder()
                                .date(date)
                                .open(new BigDecimal(values.get("1. open")))
                                .close(new BigDecimal(values.get("4. close")))
                                .high(new BigDecimal(values.get("2. high")))
                                .low(new BigDecimal(values.get("3. low")))
                                .volume(new BigDecimal(values.get("5. volume")))
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("✅ {} bougies récupérées pour {}", candles.size(), symbol);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des données Alpha Vantage pour {}", symbol, e);
        }

        return candles;
    }
}