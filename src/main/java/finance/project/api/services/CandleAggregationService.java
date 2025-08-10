package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandleAggregationService {

    public List<CandleDTO> aggregateCandles(List<CandleDTO> m1Candles, String timeframe) {

        if (m1Candles.isEmpty()) {
            log.warn("⚠️ Liste de candles vide, aucune agrégation possible");
            return Collections.emptyList();
        }

        // Détermination de la durée d'un bloc temporel en minutes
        int tfMinutes = convertTimeframeToMinutes(timeframe);
        if (tfMinutes <= 1) {
            log.info("ℹ️ Timeframe de 1min demandé, aucune agrégation effectuée.");
            return m1Candles;
        }

        m1Candles = new ArrayList<>(m1Candles);
        m1Candles.sort(Comparator.comparing(
                CandleDTO::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        // Déterminer le premier point de regroupement valide
        LocalDateTime startDate = getFirstValidStartDate(m1Candles.get(0).getDate(), timeframe);

        List<CandleDTO> filteredCandles = m1Candles.stream()
                .filter(c -> !c.getDate().isBefore(startDate))
                .collect(Collectors.toList());

        // Regroupement par période (bucket par timeframe supérieur)
        Map<LocalDateTime, List<CandleDTO>> groupedCandles = filteredCandles.stream()
                .collect(Collectors.groupingBy(c -> getBucketStartTime(c.getDate(), tfMinutes)));

        List<CandleDTO> aggregatedCandles = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<CandleDTO>> entry : groupedCandles.entrySet()) {
            LocalDateTime bucketTime = entry.getKey();
            List<CandleDTO> candlesInBucket = entry.getValue();

            int expectedCount = getExpectedCandleCountForTimeframe(timeframe);
            if (candlesInBucket.size() < expectedCount) {
                log.warn("❌ Bougie ignorée pour {} : données incomplètes ({}/{})", bucketTime, candlesInBucket.size(), expectedCount);
                continue;
            }

            CandleDTO aggregated = aggregateBucket(candlesInBucket, bucketTime, timeframe);
            aggregatedCandles.add(aggregated);
        }

        aggregatedCandles.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("✅ Agrégation complétée. {} bougies créées sur le timeframe {}", aggregatedCandles.size(), timeframe);

        return aggregatedCandles;
    }

    private int convertTimeframeToMinutes(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1min" -> 1;
            case "3min" -> 3;
            case "5min" -> 5;
            case "10min" -> 10;
            case "15min" -> 15;
            case "30min" -> 30;
            case "45min" -> 45;
            case "1h" -> 60;
            case "2h" -> 120;
            case "4h" -> 240;
            case "8h" -> 480;
            case "12h" -> 720;
            case "daily" -> 1440;
            case "weekly" -> 10080;    // 7 * 1440
            case "monthly" -> 43200;   // 30 * 1440 (approx)
            default -> {
                log.warn("⚠️ Timeframe non supporté '{}', fallback à M1", timeframe);
                yield 1;
            }
        };
    }

    private int getExpectedCandleCountForTimeframe(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "3min" -> 3;
            case "5min" -> 5;
            case "10min" -> 10;
            case "15min" -> 15;
            case "30min" -> 30;
            case "45min" -> 45;
            case "1h" -> 60;
            case "2h" -> 120;
            case "4h" -> 240;
            case "8h" -> 480;
            case "12h" -> 720;
            case "daily" -> 1440;
            case "weekly" -> 10080;
            case "monthly" -> 43200;
            default -> 1;
        };
    }

    private LocalDateTime getFirstValidStartDate(LocalDateTime firstCandleTime, String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "weekly" -> firstCandleTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
            case "monthly" -> firstCandleTime.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
            default -> firstCandleTime;
        };
    }


    private LocalDateTime getBucketStartTime(LocalDateTime dateTime, int tfMinutes) {

        if (tfMinutes < 60) {
            // Timeframes en minutes : 1min, 3min, 5min, 10min, 15min, 30min, 45min
            int minuteOfPeriod = (dateTime.getMinute() / tfMinutes) * tfMinutes;
            return dateTime.withMinute(minuteOfPeriod).withSecond(0).withNano(0);

        } else if (tfMinutes == 60) {
            // H1
            return dateTime.withMinute(0).withSecond(0).withNano(0);

        } else if (tfMinutes == 120) {
            // 2H
            int hourOfPeriod = (dateTime.getHour() / 2) * 2;
            return dateTime.withHour(hourOfPeriod).withMinute(0).withSecond(0).withNano(0);

        } else if (tfMinutes == 240) {
            // 4H → 00h, 04h, 08h, etc.
            int hourOfPeriod = (dateTime.getHour() / 4) * 4;
            return dateTime.withHour(hourOfPeriod).withMinute(0).withSecond(0).withNano(0);

        } else if (tfMinutes == 480) {
            // 8H → 00h, 08h, 16h
            int hourOfPeriod = (dateTime.getHour() / 8) * 8;
            return dateTime.withHour(hourOfPeriod).withMinute(0).withSecond(0).withNano(0);

        } else if (tfMinutes == 720) {
            // 12H → 00h, 12h
            int hourOfPeriod = (dateTime.getHour() / 12) * 12;
            return dateTime.withHour(hourOfPeriod).withMinute(0).withSecond(0).withNano(0);

        } else if (tfMinutes == 1440) {
            // D1 → début de journée
            return dateTime.toLocalDate().atStartOfDay();

        } else if (tfMinutes == 10080) {
            // Weekly → début de semaine (lundi)
            return dateTime.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();

        } else if (tfMinutes == 43200) {
            // Monthly → 1er jour du mois
            return dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay();

        } else {
            log.warn("⚠️ Timeframe non standard '{} minutes'. Fallback à date brute", tfMinutes);
            return dateTime.withSecond(0).withNano(0);
        }
    }



    private CandleDTO aggregateBucket(List<CandleDTO> candles, LocalDateTime bucketStart, String timeframe) {

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Le groupe de candles est vide pour l'agrégation !");
        }

        candles.sort(Comparator.comparing(CandleDTO::getDate));

        BigDecimal open = candles.get(0).getOpen();
        BigDecimal close = candles.get(candles.size() - 1).getClose();

        BigDecimal high = candles.stream()
                .map(CandleDTO::getHigh)
                .max(Comparator.naturalOrder())
                .orElse(open);

        BigDecimal low = candles.stream()
                .map(CandleDTO::getLow)
                .min(Comparator.naturalOrder())
                .orElse(open);

        BigDecimal totalVolume = candles.stream()
                .map(c -> c.getVolume() != null ? c.getVolume() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tu peux aussi recalculer le volumeAverage et l'openInterest si nécessaire
        return CandleDTO.builder()
                .date(bucketStart)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(totalVolume)
                .timeframe(timeframe)
                .symbol(candles.get(0).getSymbol()) // prend le premier, car on est déjà dans le même symbol
                .build();
    }
}
