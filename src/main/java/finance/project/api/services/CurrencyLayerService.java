package finance.project.api.services;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyLayerService {

    @Value("${currencylayer.api.url}")
    private String currencyLayerApiUrl;

    @Value("${currencylayer.api.key}")
    private String currencyLayerApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Récupère le taux de change EUR/USD en temps réel via CurrencyLayer.
     *
     * @return Taux actuel EUR/USD
     */
    public double getLiveExchangeRate() {
        String url = UriComponentsBuilder.fromHttpUrl(currencyLayerApiUrl + "/live")
                .queryParam("access_key", currencyLayerApiKey)
                .queryParam("currencies", "EUR")
                .queryParam("source", "USD")
                .build()
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, HashMap.class);

        if (response != null && response.containsKey("quotes")) {
            Map<String, Double> quotes = (Map<String, Double>) response.get("quotes");
            return quotes.getOrDefault("USDEUR", 0.0); // Inverse pour avoir EUR/USD
        }

        throw new RuntimeException("Impossible de récupérer les données de CurrencyLayer.");
    }

    /**
     * Récupère le taux de change EUR/USD à une date spécifique.
     *
     * @param date Date du taux de change (format YYYY-MM-DD)
     * @return Taux EUR/USD à la date spécifiée
     */
    public double getHistoricalExchangeRate(String date) {
        String url = UriComponentsBuilder.fromHttpUrl(currencyLayerApiUrl + "/historical")
                .queryParam("access_key", currencyLayerApiKey)
                .queryParam("date", date)
                .queryParam("currencies", "EUR")
                .queryParam("source", "USD")
                .build()
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, HashMap.class);

        if (response != null && response.containsKey("quotes")) {
            Map<String, Double> quotes = (Map<String, Double>) response.get("quotes");
            return quotes.getOrDefault("USDEUR", 0.0);
        }

        throw new RuntimeException("Impossible de récupérer les données historiques de CurrencyLayer.");
    }
}
