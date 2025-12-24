package finance.project.api.controllers;

import finance.project.api.model.TradeViewDTO;
import finance.project.api.services.BrokerTradeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradesController.class)
class TradesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrokerTradeService brokerTradeService;

    @Test
    void listTradesReturnsResults() throws Exception {
        given(brokerTradeService.brokerId()).willReturn("IBKR");
        given(brokerTradeService.listOpenTrades()).willReturn(List.of(
                new TradeViewDTO(
                        "IBKR",
                        "DU123",
                        "AAPL",
                        "Apple Inc.",
                        "STK",
                        "USD",
                        "SMART",
                        12345,
                        10,
                        150.0,
                        155.0,
                        1550.0,
                        50.0,
                        10.0,
                        Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()
                )
        ));

        mockMvc.perform(get("/trades")
                        .param("broker", "IBKR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].symbol", is("AAPL")))
                .andExpect(jsonPath("$[0].broker", is("IBKR")));
    }
}
