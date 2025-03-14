package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        // Regroupement par période (bucket par timeframe supérieur)
        Map<LocalDateTime, List<CandleDTO>> groupedCandles = m1Candles.stream()
                .collect(Collectors.groupingBy(candle -> getBucketStartTime(candle.getDate(), tfMinutes)));

        List<CandleDTO> aggregatedCandles = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<CandleDTO>> entry : groupedCandles.entrySet()) {
            LocalDateTime bucketTime = entry.getKey();
            List<CandleDTO> candlesInBucket = entry.getValue();

            CandleDTO aggregated = aggregateBucket(candlesInBucket, bucketTime, timeframe);
            aggregatedCandles.add(aggregated);
        }

        aggregatedCandles.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("✅ Agrégation complétée. {} bougies créées sur le timeframe {}", aggregatedCandles.size(), timeframe);

        return aggregatedCandles;
    }

    private int convertTimeframeToMinutes(String timeframe) {
        return switch (timeframe.toUpperCase()) {
            case "M1" -> 1;
            case "M5" -> 5;
            case "M15" -> 15;
            case "M30" -> 30;
            case "H1" -> 60;
            case "H4" -> 240;
            case "D1" -> 1440;
            default -> {
                log.warn("⚠️ Timeframe non supporté '{}', fallback à M1", timeframe);
                yield 1;
            }
        };
    }

    private LocalDateTime getBucketStartTime(LocalDateTime dateTime, int tfMinutes) {
        if (tfMinutes < 60) {
            // Timeframe basé sur les minutes (M5, M15, M30, etc.)
            int minuteOfPeriod = (dateTime.getMinute() / tfMinutes) * tfMinutes;
            return dateTime.withMinute(minuteOfPeriod).withSecond(0).withNano(0);
        } else if (tfMinutes == 60) {
            // H1 → On aligne à l'heure
            return dateTime.withMinute(0).withSecond(0).withNano(0);
        } else if (tfMinutes == 240) {
            // H4 → 00h, 04h, 08h, 12h, 16h, 20h
            int hourOfPeriod = (dateTime.getHour() / 4) * 4;
            return dateTime.withHour(hourOfPeriod).withMinute(0).withSecond(0).withNano(0);
        } else if (tfMinutes == 1440) {
            // D1 → début de journée
            return dateTime.toLocalDate().atStartOfDay();
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
