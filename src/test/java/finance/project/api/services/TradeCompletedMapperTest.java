package finance.project.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import finance.project.api.entities.TradeCompleted;
import finance.project.api.model.TradeCompletedDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TradeCompletedMapperTest {

    private TradeCompletedMapper tradeCompletedMapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        tradeCompletedMapper = new TradeCompletedMapper(objectMapper);
    }

    @Test
    void toDtoWithoutIntermediateEntriesReturnsEmptyList() {
        TradeCompleted trade = TradeCompleted.builder()
                .metaJson("{\"foo\":\"bar\"}")
                .build();

        TradeCompletedDTO dto = tradeCompletedMapper.toDto(trade);

        assertThat(dto.intermediateEntries()).isEmpty();
    }

    @Test
    void toDtoWithEmptyIntermediateEntriesReturnsEmptyList() {
        TradeCompleted trade = TradeCompleted.builder()
                .metaJson("{\"intermediate_entries\":[]}")
                .build();

        TradeCompletedDTO dto = tradeCompletedMapper.toDto(trade);

        assertThat(dto.intermediateEntries()).isEmpty();
    }

    @Test
    void toDtoWithIntermediateEntriesMapsFields() {
        TradeCompleted trade = TradeCompleted.builder()
                .metaJson("{\"intermediate_entries\":[{\"index\":1,\"ts_utc\":\"2025-04-08T00:00:00+00:00\",\"qty\":0.2,\"price\":172.42,\"grid_level\":-30.0,\"dd_pct\":-30.2225,\"palier_used\":0.2}]}")
                .build();

        TradeCompletedDTO dto = tradeCompletedMapper.toDto(trade);

        assertThat(dto.intermediateEntries()).hasSize(1);
        var entry = dto.intermediateEntries().getFirst();
        assertThat(entry.index()).isEqualTo(1);
        assertThat(entry.tsUtc()).isEqualTo(OffsetDateTime.parse("2025-04-08T00:00:00+00:00"));
        assertThat(entry.qty()).isEqualTo(0.2);
        assertThat(entry.price()).isEqualTo(172.42);
        assertThat(entry.gridLevel()).isEqualTo(-30.0);
        assertThat(entry.ddPct()).isEqualTo(-30.2225);
        assertThat(entry.palierUsed()).isEqualTo(0.2);
    }
}
