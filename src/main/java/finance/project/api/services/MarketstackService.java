package finance.project.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketstackService {

    @Value("${marketstack.api.url}")
    private String marketstackApiUrl;

    @Value("${marketstack.api.key}")
    private String marketstackApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Récupère les données historiques EUR/USD (CME) en Daily via Marketstack.
     *
     * @return Liste des données journalières (date, open, high, low, close, volume)
     */
    public List<Map<String, Object>> getHistoricalEURUSD() {
        String url = UriComponentsBuilder.fromHttpUrl(marketstackApiUrl + "/eod")
                .queryParam("access_key", marketstackApiKey)
                .queryParam("symbols", "6E1!")
                .queryParam("exchange", "XCEC")
                .queryParam("limit", 100)
                .build()
                .toUriString();

        // Appel API et récupération des données
        Map<String, Object> response = restTemplate.getForObject(url, HashMap.class);
        return (List<Map<String, Object>>) response.get("data");
    }
}

