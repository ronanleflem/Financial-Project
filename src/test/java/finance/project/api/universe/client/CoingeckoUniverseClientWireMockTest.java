package finance.project.api.universe.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

class CoingeckoUniverseClientWireMockTest {

    private static final String API_KEY = "test-key";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void fetchTopCryptoSymbols_returnsSymbolsFromWireMock() {
        stubFor(get(urlPathEqualTo("/coins/markets"))
                .withQueryParam("vs_currency", equalTo("usd"))
                .withQueryParam("order", equalTo("market_cap_desc"))
                .withQueryParam("per_page", equalTo("2"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("sparkline", equalTo("false"))
                .withHeader("x-cg-demo-api-key", equalTo(API_KEY))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                  {"id": "bitcoin", "symbol": "btc", "name": "Bitcoin"},
                                  {"id": "ethereum", "symbol": "eth", "name": "Ethereum"}
                                ]
                                """)));

        CoingeckoUniverseClient client = new CoingeckoUniverseClient(
                wireMock.getRuntimeInfo().getHttpBaseUrl(),
                API_KEY,
                new RestTemplate()
        );

        List<String> symbols = client.fetchTopCryptoSymbols(2);

        assertThat(symbols).containsExactly("BTC", "ETH");
        verify(getRequestedFor(urlPathEqualTo("/coins/markets"))
                .withHeader("x-cg-demo-api-key", equalTo(API_KEY)));
    }

    @Test
    void fetchTopCryptoSymbols_returnsEmptyListOnTimeout() {
        stubFor(get(urlPathEqualTo("/coins/markets"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withFixedDelay(500)
                        .withBody("[]")));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(100);
        requestFactory.setReadTimeout(100);

        CoingeckoUniverseClient client = new CoingeckoUniverseClient(
                wireMock.getRuntimeInfo().getHttpBaseUrl(),
                API_KEY,
                new RestTemplate(requestFactory)
        );

        List<String> symbols = client.fetchTopCryptoSymbols(1);

        assertThat(symbols).isEmpty();
    }
}
