package finance.project.api.services;

import finance.project.api.enums.MarketType;
import finance.project.api.model.CandleDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandleAggregationServiceTest {

    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final CandleAggregationService service = new CandleAggregationService();

    @Test
    void findMissingM1CandlesCmeAccountsForDailyBreakAndDuplicates() {
        ZonedDateTime startChi = ZonedDateTime.of(2024, 6, 3, 15, 58, 0, 0, CHICAGO);
        ZonedDateTime endChi = ZonedDateTime.of(2024, 6, 3, 17, 2, 0, 0, CHICAGO);
        LocalDateTime startUtc = startChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = endChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        List<CandleDTO> candles = new ArrayList<>();
        candles.add(candleAt(startChi, 100, 101, 99, 100, 10));
        candles.add(candleAt(startChi.plusMinutes(1), 100, 102, 98, 101, 11));
        candles.add(candleAt(startChi.plusMinutes(1), 101, 103, 99, 102, 12));
        candles.add(candleAt(startChi.plusMinutes(62), 102, 104, 100, 103, 13));

        CandleAggregationService.MissingM1Report report =
                service.findMissingM1CandlesCME(candles, startUtc, endUtc);

        LocalDateTime missingUtc = startChi.plusMinutes(63)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime duplicateUtc = startChi.plusMinutes(1)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        assertEquals(List.of(missingUtc), report.missingMinutes);
        assertEquals(List.of(duplicateUtc), report.duplicateMinutes);
        assertEquals(4, report.expectedCount);
        assertEquals(3, report.actualUniqueCount);
        assertEquals(75.0, report.coveragePct, 0.01);
    }

    @Test
    void findMissingM1CandlesCmeSkipsWeekendClosure() {
        ZonedDateTime startChi = ZonedDateTime.of(2024, 6, 7, 15, 58, 0, 0, CHICAGO);
        ZonedDateTime endChi = ZonedDateTime.of(2024, 6, 9, 17, 2, 0, 0, CHICAGO);
        LocalDateTime startUtc = startChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = endChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        List<CandleDTO> candles = List.of(
                candleAt(startChi, 200, 201, 199, 200, 20),
                candleAt(startChi.plusMinutes(1), 200, 202, 198, 201, 21),
                candleAt(ZonedDateTime.of(2024, 6, 9, 17, 0, 0, 0, CHICAGO), 201, 203, 200, 202, 22),
                candleAt(ZonedDateTime.of(2024, 6, 9, 17, 1, 0, 0, CHICAGO), 202, 204, 201, 203, 23)
        );

        CandleAggregationService.MissingM1Report report =
                service.findMissingM1CandlesCME(candles, startUtc, endUtc);

        assertTrue(report.missingMinutes.isEmpty());
        assertTrue(report.duplicateMinutes.isEmpty());
        assertEquals(4, report.expectedCount);
        assertEquals(4, report.actualUniqueCount);
        assertEquals(100.0, report.coveragePct, 0.01);
    }

    @Test
    void checkMissingM1BeforeAggregationEnforcesCoverageThreshold() {
        ZonedDateTime baseChi = ZonedDateTime.of(2024, 6, 3, 12, 0, 0, 0, CHICAGO);
        List<CandleDTO> candles = List.of(
                candleAt(baseChi, 300, 301, 299, 300, 30),
                candleAt(baseChi.plusMinutes(2), 301, 302, 298, 299, 31)
        );

        CandleAggregationService.MissingM1Report report =
                service.checkMissingM1BeforeAggregation(candles, null, null, 0);

        assertEquals(3, report.expectedCount);
        assertEquals(2, report.actualUniqueCount);
        assertEquals(66.67, report.coveragePct, 0.1);

        assertThrows(IllegalStateException.class,
                () -> service.checkMissingM1BeforeAggregation(candles, null, null, 90));
    }

    @Test
    void aggregateCandlesBuildsBucketsWithExpectedOhlc() {
        ZonedDateTime startParis = ZonedDateTime.of(2024, 6, 4, 14, 0, 0, 0, PARIS);
        List<CandleDTO> candles = List.of(
                candleAt(startParis.plusMinutes(0), 100, 105, 95, 102, 10),
                candleAt(startParis.plusMinutes(1), 102, 108, 101, 107, 11),
                candleAt(startParis.plusMinutes(2), 107, 109, 106, 108, 12),
                candleAt(startParis.plusMinutes(3), 108, 110, 107, 109, 13),
                candleAt(startParis.plusMinutes(4), 109, 111, 108, 110, 14),
                candleAt(startParis.plusMinutes(5), 110, 112, 109, 111, 15),
                candleAt(startParis.plusMinutes(6), 111, 113, 110, 112, 16),
                candleAt(startParis.plusMinutes(7), 112, 114, 111, 113, 17),
                candleAt(startParis.plusMinutes(8), 113, 115, 112, 114, 18),
                candleAt(startParis.plusMinutes(9), 114, 116, 113, 115, 19)
        );

        List<CandleDTO> aggregated = service.aggregateCandles(candles, "5min", MarketType.CME, true);

        assertEquals(2, aggregated.size());

        LocalDateTime firstBucketStart = startParis.withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime().withSecond(0).withNano(0);
        LocalDateTime secondBucketStart = firstBucketStart.plusMinutes(5);

        CandleDTO first = aggregated.get(0);
        CandleDTO second = aggregated.get(1);

        assertEquals(firstBucketStart, first.getDate());
        assertEquals(BigDecimal.valueOf(100), first.getOpen());
        assertEquals(BigDecimal.valueOf(110), first.getClose());
        assertEquals(BigDecimal.valueOf(111), first.getHigh());
        assertEquals(BigDecimal.valueOf(95), first.getLow());
        assertEquals(BigDecimal.valueOf(60), first.getVolume());

        assertEquals(secondBucketStart, second.getDate());
        assertEquals(BigDecimal.valueOf(110), second.getOpen());
        assertEquals(BigDecimal.valueOf(115), second.getClose());
        assertEquals(BigDecimal.valueOf(116), second.getHigh());
        assertEquals(BigDecimal.valueOf(109), second.getLow());
        assertEquals(BigDecimal.valueOf(85), second.getVolume());
    }

    private static CandleDTO candleAt(ZonedDateTime time,
                                      double open,
                                      double high,
                                      double low,
                                      double close,
                                      double volume) {
        return CandleDTO.builder()
                .date(time.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .open(BigDecimal.valueOf(open))
                .high(BigDecimal.valueOf(high))
                .low(BigDecimal.valueOf(low))
                .close(BigDecimal.valueOf(close))
                .volume(BigDecimal.valueOf(volume))
                .timeframe("1min")
                .build();
    }
}
