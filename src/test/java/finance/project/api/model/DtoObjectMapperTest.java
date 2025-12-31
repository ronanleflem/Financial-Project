package finance.project.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoObjectMapperTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void tradeSignalDtoRoundTripUsesIsoTimestamp() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2024, 2, 3, 4, 5, 6);
        TradeSignalDTO dto = TradeSignalDTO.builder()
                .tradeType(TradeSignalDTO.TradeType.LONG)
                .entryPrice(101.12)
                .stopLoss(95.50)
                .takeProfit(120.75)
                .confidenceScore(0.92)
                .timestamp(timestamp)
                .secondEntry(true)
                .symbol("AAPL")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("timestamp").asText()).isEqualTo("2024-02-03T04:05:06");

        TradeSignalDTO restored = objectMapper.readValue(json, TradeSignalDTO.class);

        assertThat(restored.getTradeType()).isEqualTo(dto.getTradeType());
        assertThat(restored.getEntryPrice()).isEqualTo(dto.getEntryPrice());
        assertThat(restored.getStopLoss()).isEqualTo(dto.getStopLoss());
        assertThat(restored.getTakeProfit()).isEqualTo(dto.getTakeProfit());
        assertThat(restored.getConfidenceScore()).isEqualTo(dto.getConfidenceScore());
        assertThat(restored.getTimestamp()).isEqualTo(dto.getTimestamp());
        assertThat(restored.isSecondEntry()).isEqualTo(dto.isSecondEntry());
        assertThat(restored.getSymbol()).isEqualTo(dto.getSymbol());
    }

    @Test
    void tradeCompletedDtoRoundTripPreservesValues() throws Exception {
        LocalDateTime entryTimestamp = LocalDateTime.of(2024, 1, 1, 10, 15, 30);
        LocalDateTime exitTimestamp = LocalDateTime.of(2024, 1, 2, 11, 45, 0);
        OffsetDateTime entryOffset = OffsetDateTime.parse("2024-01-01T10:00:00+01:00");
        IntermediateEntryDTO intermediateEntry = new IntermediateEntryDTO(
                1,
                entryOffset,
                0.2,
                172.42,
                -30.0,
                -30.2225,
                0.2
        );

        TradeCompletedDTO dto = new TradeCompletedDTO(
                10L,
                "Momentum",
                "run-123",
                TradeSignalDTO.TradeType.SHORT,
                "Equity",
                100.5,
                98.2,
                110.75,
                105.3,
                4.8,
                new BigDecimal("1.2345"),
                new BigDecimal("-2.5000"),
                new BigDecimal("3.333"),
                4,
                0.88,
                entryTimestamp,
                exitTimestamp,
                "AAPL",
                List.of(intermediateEntry)
        );

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("entryTimestamp").asText()).isEqualTo("2024-01-01T10:15:30");
        assertThat(node.get("exitTimestamp").asText()).isEqualTo("2024-01-02T11:45:00");
        assertThat(node.get("intermediateEntries").get(0).get("tsUtc").asText())
                .isEqualTo("2024-01-01T10:00:00+01:00");

        TradeCompletedDTO restored = objectMapper.readValue(json, TradeCompletedDTO.class);

        assertThat(restored).isEqualTo(dto);
        assertThat(restored.pnlPct()).isEqualByComparingTo(new BigDecimal("1.2345"));
        assertThat(restored.maxDrawdownPct()).isEqualByComparingTo(new BigDecimal("-2.5000"));
        assertThat(restored.quantity()).isEqualByComparingTo(new BigDecimal("3.333"));
    }

    @Test
    void candleDtoDeserializesOptionalFieldsAndKeepsPrecision() throws Exception {
        String json = """
                {
                  "timeframe": "1D",
                  "symbolFuture": "ES",
                  "date": "2024-05-10T09:30:00",
                  "open": 1.0000000001,
                  "close": 2.50
                }
                """;

        CandleDTO dto = objectMapper.readValue(json, CandleDTO.class);

        assertThat(dto.getTimeframe()).isEqualTo("1D");
        assertThat(dto.getSymbolFuture()).isEqualTo("ES");
        assertThat(dto.getDate()).isEqualTo(LocalDateTime.of(2024, 5, 10, 9, 30));
        assertThat(dto.getOpen()).isEqualByComparingTo(new BigDecimal("1.0000000001"));
        assertThat(dto.getClose()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(dto.getVolume()).isNull();
        assertThat(dto.getVolumeAverage()).isNull();
        assertThat(dto.getOpenInterest()).isNull();

        String serialized = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(serialized);

        assertThat(node.get("date").asText()).isEqualTo("2024-05-10T09:30:00");
    }
}
