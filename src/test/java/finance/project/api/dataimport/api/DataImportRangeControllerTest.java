package finance.project.api.dataimport.api;

import finance.project.api.dataimport.dto.DeltaIngestionRangeResponse;
import finance.project.api.dataimport.infrastructure.DeltaIngestionRangeService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataImportRangeController.class)
class DataImportRangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeltaIngestionRangeService deltaIngestionRangeService;

    @Test
    void returnsRangesForAngular() throws Exception {
        DeltaIngestionRangeResponse row = new DeltaIngestionRangeResponse(
                "BTCUSDT",
                "CRYPTO",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-10T00:00:00Z"),
                "1h",
                Instant.parse("2026-02-20T10:00:00Z")
        );
        given(deltaIngestionRangeService.findRanges("BTCUSDT", "CRYPTO", "1h", 50))
                .willReturn(List.of(row));

        mockMvc.perform(get("/api/data-import/ranges")
                        .param("symbol", "BTCUSDT")
                        .param("insertedType", "CRYPTO")
                        .param("timeframe", "1h")
                        .param("limit", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].symbol", is("BTCUSDT")))
                .andExpect(jsonPath("$[0].insertedType", is("CRYPTO")))
                .andExpect(jsonPath("$[0].timeframe", is("1h")));

        verify(deltaIngestionRangeService).findRanges("BTCUSDT", "CRYPTO", "1h", 50);
    }
}

