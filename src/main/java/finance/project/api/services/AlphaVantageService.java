package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlphaVantageService {

    private static final String API_KEY = "4NBH950DAGIXA6LV";
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final String INTERVAL = "5min";


    private final RestTemplate restTemplate;

    public AlphaVantageService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public List<CandleDTO> getHistoricalData(String symbol) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("function", "TIME_SERIES_INTRADAY")
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", API_KEY)
                    .queryParam("interval", INTERVAL)
                    .queryParam("outputsize", "compact")
                    .toUriString();

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> response = responseEntity.getBody();

            if (response == null || !response.containsKey("Time Series ("+INTERVAL+")")) {
                log.error("❌ Impossible de récupérer les données pour {}", symbol);
                return new ArrayList<>();
            }

            Map<String, Map<String, String>> timeSeries = (Map<String, Map<String, String>>) response.get("Time Series ("+INTERVAL+")");

            return timeSeries.entrySet().stream()
                    .map(entry -> {
                        Map<String, String> values = entry.getValue();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                        return CandleDTO.builder()
                                .date(LocalDateTime.parse(entry.getKey(), formatter))
                                .open(new BigDecimal(values.get("1. open")))
                                .close(new BigDecimal(values.get("4. close")))
                                .high(new BigDecimal(values.get("2. high")))
                                .low(new BigDecimal(values.get("3. low")))
                                .volume(new BigDecimal(values.get("5. volume")))
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des données Alpha Vantage pour {}", symbol, e);
            return new ArrayList<>();
        }
    }
}