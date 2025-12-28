package finance.project.api.live;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private LiveTradeAckRepository repository;

  @MockBean private LiveSseHub liveSseHub;

  @BeforeEach
  void setup() {
    repository.deleteAll();
    reset(liveSseHub);
  }

  @Test
  void shouldAcceptSignalAndIgnoreDuplicate() throws Exception {
    LiveSignalDTO dto = baseDto();

    mockMvc
        .perform(
            post("/live/signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("accepted"));

    mockMvc
        .perform(
            post("/live/signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("duplicate_ignored"));

    verify(liveSseHub, times(2)).broadcast(any(LiveTradeAck.class));
  }

  @Test
  void shouldReturnRecentSignals() throws Exception {
    LiveSignalDTO dto = baseDto();
    mockMvc
        .perform(
            post("/live/signal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(dto)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/live/recent").param("lookback", "PT24H"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].strategyId").value(dto.strategyId()));
  }

  private LiveSignalDTO baseDto() {
    return new LiveSignalDTO(
        "strat-1",
        "EURUSD",
        "M15",
        Instant.now(),
        "LONG",
        1.2345,
        1.2000,
        1.3000,
        2.5,
        Map.of("note", "test"));
  }
}
