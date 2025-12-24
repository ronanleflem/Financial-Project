package finance.project.api.controllers;

import finance.project.api.model.market.MarketScanItem;
import finance.project.api.model.market.OhlcBar;
import finance.project.api.services.market.HistoricalDataService;
import finance.project.api.services.market.MarketScannerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketScannerController.class)
class MarketScannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketScannerService marketScannerService;

    @MockBean
    private HistoricalDataService historicalDataService;

    @Test
    void scanTopLosersReturnsItems() throws Exception {
        given(marketScannerService.scanTopLosersByRegion("US", "EQUITY", 1_000_000.0, 5))
                .willReturn(List.of(
                        new MarketScanItem("AAPL", "Apple", "NASDAQ", 150.0, -2.5, 2_000_000_000.0, "USD", "TECH", null, null)
                ));

        mockMvc.perform(get("/api/market/scans/top-losers")
                        .param("region", "US")
                        .param("assetClass", "EQUITY")
                        .param("minMarketCap", "1000000")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[0].changePct", is(-2.5)));
    }

    @Test
    void getOhlcReturnsBars() throws Exception {
        given(historicalDataService.getHistoricalOhlc(anyString(), any(), any(), any(), any()))
                .willReturn(List.of(
                        new OhlcBar(Instant.parse("2024-01-01T00:00:00Z"), 100.0, 110.0, 95.0, 105.0, 1000.0)
                ));

        mockMvc.perform(get("/api/market/ohlc")
                        .param("symbol", "AAPL")
                        .param("start", "2024-01-01T00:00:00Z")
                        .param("end", "2024-01-02T00:00:00Z")
                        .param("timeframe", "1d")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].open", is(100.0)))
                .andExpect(jsonPath("$[0].close", is(105.0)));
    }

    @Test
    void getOhlcReturnsBadRequestOnInvalidDate() throws Exception {
        mockMvc.perform(get("/api/market/ohlc")
                        .param("symbol", "AAPL")
                        .param("start", "invalid-date")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid date format. Use ISO-8601.")));
    }
}
