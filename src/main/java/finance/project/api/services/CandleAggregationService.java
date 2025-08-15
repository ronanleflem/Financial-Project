package finance.project.api.services;

import finance.project.api.model.CandleDTO;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
public class CandleAggregationService {

    public static class MissingM1Report {
        public final List<LocalDateTime> missingMinutes;
        public final List<LocalDateTime> duplicateMinutes;
        public final int expectedCount;
        public final int actualUniqueCount;
        public final double coveragePct;

        public MissingM1Report(List<LocalDateTime> missingMinutes,
                               List<LocalDateTime> duplicateMinutes,
                               int expectedCount,
                               int actualUniqueCount) {
            this.missingMinutes = missingMinutes;
            this.duplicateMinutes = duplicateMinutes;
            this.expectedCount = expectedCount;
            this.actualUniqueCount = actualUniqueCount;
            this.coveragePct = expectedCount == 0 ? 100.0 : (100.0 * actualUniqueCount / expectedCount);
        }
    }

    // -------- Règles CME Globex FX (6E) --------
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("America/Chicago");
    private static final LocalTime DAILY_BREAK_START = LocalTime.of(16, 0); // 16:00
    private static final LocalTime DAILY_BREAK_END   = LocalTime.of(17, 0); // 17:00
    private static final LocalTime SUNDAY_OPEN       = LocalTime.of(17, 0); // dim 17:00
    private static final LocalTime FRIDAY_CLOSE      = LocalTime.of(16, 0); // ven 16:00

    private boolean isTradableMinuteCME(ZonedDateTime zdt) {
        DayOfWeek dow = zdt.getDayOfWeek();
        LocalTime t = zdt.toLocalTime();

        if (dow == DayOfWeek.SATURDAY) return false;
        if (!t.isBefore(DAILY_BREAK_START) && t.isBefore(DAILY_BREAK_END)) return false; // 16:00–17:00
        if (dow == DayOfWeek.SUNDAY) return !t.isBefore(SUNDAY_OPEN);   // dim >= 17:00
        if (dow == DayOfWeek.FRIDAY) return t.isBefore(FRIDAY_CLOSE);   // ven < 16:00
        return true; // lun–jeu hors pause
    }

    /**
     * Construit le rapport des minutes manquantes/doublons entre startInclusive et endExclusive.
     * Les minutes attendues respectent le calendrier CME (America/Chicago + pause quotidienne).
     */
    public MissingM1Report findMissingM1CandlesCME(List<CandleDTO> m1Candles,
                                                   LocalDateTime startInclusive,
                                                   LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Fenêtre temporelle invalide");
        }
        if (m1Candles == null) m1Candles = Collections.emptyList();

        // minutes réelles (arrondies à la minute)
        List<LocalDateTime> actualMinutes = m1Candles.stream()
                .filter(Objects::nonNull)
                .map(CandleDTO::getDate)
                .filter(Objects::nonNull)
                .map(dt -> dt.withSecond(0).withNano(0))
                .toList();

        Map<LocalDateTime, Long> counts = actualMinutes.stream()
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        List<LocalDateTime> duplicateMinutes = counts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        Set<LocalDateTime> actualUnique = counts.keySet();

        // on parcourt en UTC et on teste la tradabilité en Chicago
        Set<LocalDateTime> expected = new LinkedHashSet<>();
        ZonedDateTime zCurUtc = startInclusive.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        ZonedDateTime zEndUtc = endExclusive.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        while (zCurUtc.isBefore(zEndUtc)) {
            ZonedDateTime zChi = zCurUtc.withZoneSameInstant(EXCHANGE_ZONE);
            if (isTradableMinuteCME(zChi)) {
                expected.add(zCurUtc.toLocalDateTime()); // on stocke la minute attendue en UTC
            }
            zCurUtc = zCurUtc.plusMinutes(1);
        }

        List<LocalDateTime> missing = expected.stream()
                .filter(min -> !actualUnique.contains(min))
                .sorted()
                .toList();

        return new MissingM1Report(missing, duplicateMinutes, expected.size(), actualUnique.size());
    }

    /**
     * Helper pratique : à appeler AVANT l'agrégation.
     * - Déduit la fenêtre [start,end) depuis les données si non fournie
     * - Loggue un résumé
     * - Optionnellement bloque si trop de trous
     *
     * @param hardFailIfCoverageBelow  ex: 98.0 = échouer si couverture < 98%
     */
    public MissingM1Report checkMissingM1BeforeAggregation(List<CandleDTO> m1Candles,
                                                           @Nullable LocalDateTime startInclusive,
                                                           @Nullable LocalDateTime endExclusive,
                                                           double hardFailIfCoverageBelow) {
        if (m1Candles == null || m1Candles.isEmpty()) {
            log.warn("⚠️ Aucune M1 fournie pour le contrôle des trous.");
            return new MissingM1Report(List.of(), List.of(), 0, 0);
        }

        // déduction auto de la fenêtre si besoin
        LocalDateTime minDt = m1Candles.stream().map(CandleDTO::getDate).filter(Objects::nonNull).min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime maxDt = m1Candles.stream().map(CandleDTO::getDate).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElseThrow();
        if (startInclusive == null) startInclusive = minDt.withSecond(0).withNano(0);
        if (endExclusive == null)   endExclusive   = maxDt.plusMinutes(1).withSecond(0).withNano(0); // exclusif

        MissingM1Report rpt = findMissingM1CandlesCME(m1Candles, startInclusive, endExclusive);

        log.info("🧪 Check M1 CME {} -> {} | attendu={} min, reçu={} uniques, couverture={}% | missing={}, duplicates={}",
                startInclusive, endExclusive, rpt.expectedCount, rpt.actualUniqueCount,
                String.format(Locale.US, "%.2f", rpt.coveragePct),
                rpt.missingMinutes.size(), rpt.duplicateMinutes.size());

        if (!rpt.missingMinutes.isEmpty()) {
            LocalDateTime first = rpt.missingMinutes.get(0);
            LocalDateTime last  = rpt.missingMinutes.get(rpt.missingMinutes.size()-1);
            log.warn("⛳ Exemples minutes manquantes: first={}, last={}, total={}", first, last, rpt.missingMinutes.size());
        }
        if (!rpt.duplicateMinutes.isEmpty()) {
            log.warn("🔁 Exemples minutes en doublon: {}", rpt.duplicateMinutes.get(0));
        }

        if (hardFailIfCoverageBelow > 0 && rpt.coveragePct < hardFailIfCoverageBelow) {
            throw new IllegalStateException("Couverture M1 insuffisante: " +
                    String.format(Locale.US, "%.2f", rpt.coveragePct) + "% < " + hardFailIfCoverageBelow + "%");
        }
        return rpt;
    }

    private LocalDateTime alignToCmeTradingDayStart(LocalDateTime utc) {
        ZonedDateTime z = utc.atZone(ZoneOffset.UTC);
        ZonedDateTime chi = z.withZoneSameInstant(EXCHANGE_ZONE);
        // début de la journée de trading CME = 17:00 heure de Chicago
        LocalDate d = chi.toLocalDate();
        LocalDateTime chiStart = LocalDateTime.of(d, LocalTime.of(17,0));
        if (chi.toLocalTime().isBefore(LocalTime.of(17,0))) {
            chiStart = chiStart.minusDays(1);
        }
        return chiStart.atZone(EXCHANGE_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime cmeMonthlyAnchorStartUtc(LocalDateTime anyUtcInMonth) {
        ZonedDateTime chi = anyUtcInMonth.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
        // 1er du mois en heure de Chicago
        LocalDate first = chi.toLocalDate().withDayOfMonth(1);
        // Premier DIMANCHE >= 1er du mois, à 17:00 CT
        ZonedDateTime startChi = ZonedDateTime.of(
                first.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                LocalTime.of(17, 0),
                EXCHANGE_ZONE
        );
        return startChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime cmeMonthlyAnchorEndUtc(LocalDateTime anyUtcInMonth) {
        ZonedDateTime chi = anyUtcInMonth.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
        LocalDate firstNext = chi.toLocalDate().withDayOfMonth(1).plusMonths(1);
        // Premier DIMANCHE >= 1er du mois suivant, 17:00 CT
        ZonedDateTime endChi = ZonedDateTime.of(
                firstNext.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
                LocalTime.of(17, 0),
                EXCHANGE_ZONE
        );
        return endChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Donne le début du bucket monthly contenant/immédiatement avant `dtUtc`. */
    private LocalDateTime getMonthlyBucketStartUtc(LocalDateTime dtUtc) {
        LocalDateTime anchorStart = cmeMonthlyAnchorStartUtc(dtUtc);
        LocalDateTime anchorEnd   = cmeMonthlyAnchorEndUtc(dtUtc);
        // Si dtUtc < anchorStart → on est encore “dans” le mois précédent côté sessions CME
        if (dtUtc.isBefore(anchorStart)) {
            // prendre l’ancre du mois précédent
            return cmeMonthlyAnchorStartUtc(dtUtc.minusDays(10)); // n’importe quelle date dans le mois précédent
        }
        // sinon on est ≥ anchorStart et < anchorEnd → on retourne anchorStart
        return anchorStart;
    }

    /** Début de semaine CME (dim 17:00 CT) en UTC pour n'importe quel instant UTC donné. */
    private LocalDateTime cmeWeekAnchorStartUtc(LocalDateTime anyUtcInWeek) {
        ZonedDateTime chi = anyUtcInWeek.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
        // aller au DIMANCHE local de la semaine courante
        ZonedDateTime startChi = chi.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .withHour(17).withMinute(0).withSecond(0).withNano(0);
        return startChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Début de la semaine CME suivante (dim 17:00 CT) en UTC. */
    private LocalDateTime cmeNextWeekAnchorStartUtc(LocalDateTime weekAnchorStartUtc) {
        ZonedDateTime chi = weekAnchorStartUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
        ZonedDateTime nextChi = chi.plusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .withHour(17).withMinute(0).withSecond(0).withNano(0);
        return nextChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
    private int expectedTradableCountInRange(LocalDateTime startUtc, LocalDateTime endUtc) {
        int count = 0;
        ZonedDateTime z = startUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        ZonedDateTime zEnd = endUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        while (z.isBefore(zEnd)) {
            if (isTradableMinuteCME(z.withZoneSameInstant(EXCHANGE_ZONE))) count++;
            z = z.plusMinutes(1);
        }
        return count;
    }
    public List<CandleDTO> aggregateCandles(List<CandleDTO> m1Candles, String timeframe) {

        LocalDateTime windowStart = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo).orElseThrow()
                .withSecond(0).withNano(0);

        LocalDateTime windowEndExclusive = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElseThrow()
                .plusMinutes(1).withSecond(0).withNano(0);

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
        MissingM1Report rpt = checkMissingM1BeforeAggregation(
                m1Candles,
                null,
                null,
                0
        );

        if (rpt.missingMinutes.size() > 0) {
            log.warn("🚧 Agrégation annulée : minutes manquantes détectées ({})", rpt.missingMinutes.size());
            return Collections.emptyList();
        }

        if ("weekly".equalsIgnoreCase(timeframe)) {
            return aggregateWeeklyCandles(m1Candles);
        }
        if ("monthly".equalsIgnoreCase(timeframe)) {
            return aggregateMonthlyCandles(m1Candles);
        }


        m1Candles = new ArrayList<>(m1Candles);
        m1Candles.sort(Comparator.comparing(
                CandleDTO::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        // Déterminer le premier point de regroupement valide
        LocalDateTime startDate = getFirstValidStartDate(windowStart, timeframe);
        LocalDateTime firstBucket = getBucketStartTime(windowStart, tfMinutes);
        if (firstBucket.isBefore(windowStart)) {
            firstBucket = firstBucket.plusMinutes(tfMinutes);
        }
        LocalDateTime finalFirstBucket = firstBucket;
        List<CandleDTO> filteredCandles = m1Candles.stream()
                .filter(c -> !getBucketStartTime(c.getDate(), tfMinutes).isBefore(finalFirstBucket))
                .toList();

        // Regroupement par période (bucket par timeframe supérieur)
        Map<LocalDateTime, List<CandleDTO>> groupedCandles = filteredCandles.stream()
                .collect(Collectors.groupingBy(c -> getBucketStartTime(c.getDate(), tfMinutes)));

        List<CandleDTO> aggregatedCandles = new ArrayList<>();
        int aggregIgnored = 0;
        for (Map.Entry<LocalDateTime, List<CandleDTO>> entry : groupedCandles.entrySet()) {
            LocalDateTime bucketTime = entry.getKey();
            List<CandleDTO> candlesInBucket = entry.getValue();

            LocalDateTime bucketEnd = bucketTime.plusMinutes(tfMinutes);
            if (bucketEnd.isAfter(windowEndExclusive)) {
                continue;
            }

            int expectedCount = expectedTradableCountInBucket(bucketTime, tfMinutes);
            if (candlesInBucket.size() < expectedCount) {
                aggregIgnored++;
                log.warn("❌ Bougie ignorée pour {} : données incomplètes ({}/{})", bucketTime, candlesInBucket.size(), expectedCount);
                continue;
            }

            CandleDTO aggregated = aggregateBucket(candlesInBucket, bucketTime, timeframe);
            aggregatedCandles.add(aggregated);
        }

        aggregatedCandles.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("✅ Agrégation complétée. {} bougies créées sur le timeframe {}", aggregatedCandles.size(), timeframe);
        if(aggregatedCandles.size() > 1) {
            log.info("⚠️ Agrégation ignorée. {} données incomplètes sur le timeframe {}", aggregIgnored, timeframe);
        }

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

    private LocalDateTime getFirstValidStartDate(LocalDateTime firstCandleTime, String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "daily"   -> alignToCmeTradingDayStart(firstCandleTime);
            case "weekly"  -> {
                LocalDateTime d0 = alignToCmeTradingDayStart(firstCandleTime);
                // semaine CME = dim 17:00 CT → on recule jusqu’au dernier dimanche 17:00
                ZonedDateTime chi = d0.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
                ZonedDateTime startOfWeekChi = chi.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                        .withHour(17).withMinute(0).withSecond(0).withNano(0);
                yield startOfWeekChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            case "monthly" -> {
                LocalDateTime d0 = alignToCmeTradingDayStart(firstCandleTime);
                ZonedDateTime chi = d0.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
                ZonedDateTime monthStartChi = chi.with(TemporalAdjusters.firstDayOfMonth())
                        .withHour(17).withMinute(0).withSecond(0).withNano(0);
                if (monthStartChi.isBefore(chi)) {
                    monthStartChi = monthStartChi.plusMonths(1)
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .withHour(17).withMinute(0).withSecond(0).withNano(0);
                }
                yield monthStartChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            default -> firstCandleTime;
        };
    }

    private List<CandleDTO> aggregateMonthlyCandles(List<CandleDTO> m1Candles) {

        if (m1Candles.isEmpty()) {
            log.warn("⚠️ Liste vide, aucune agrégation monthly possible");
            return Collections.emptyList();
        }

        // Bornes globales de la série
        LocalDateTime windowStart = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo).orElseThrow()
                .withSecond(0).withNano(0);

        LocalDateTime windowEndExclusive = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElseThrow()
                .plusMinutes(1).withSecond(0).withNano(0);

        // Tri
        m1Candles = new ArrayList<>(m1Candles);
        m1Candles.sort(Comparator.comparing(CandleDTO::getDate));

        // Map bucketStart → bougies
        Map<LocalDateTime, List<CandleDTO>> grouped = new HashMap<>();
        for (CandleDTO c : m1Candles) {
            LocalDateTime t = c.getDate().withSecond(0).withNano(0);
            LocalDateTime anchor = getMonthlyBucketStartUtc(t);
            grouped.computeIfAbsent(anchor, k -> new ArrayList<>()).add(c);
        }

        List<CandleDTO> aggregated = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<CandleDTO>> e : grouped.entrySet()) {
            LocalDateTime bucketStart = e.getKey();
            LocalDateTime bucketEnd   = cmeMonthlyAnchorEndUtc(bucketStart); // 1er dim ≥ 1er mois suivant 17:00 CT (en UTC)

            // borne par la fenêtre de données réellement disponible
            LocalDateTime effectiveStart = bucketStart.isBefore(windowStart) ? windowStart : bucketStart;
            LocalDateTime effectiveEnd   = bucketEnd.isAfter(windowEndExclusive) ? windowEndExclusive : bucketEnd;

            // si l'intersection est vide, on saute
            if (!effectiveStart.isBefore(effectiveEnd)) continue;

            List<CandleDTO> candlesInBucket = e.getValue().stream()
                    .filter(c -> !c.getDate().isBefore(effectiveStart) && c.getDate().isBefore(effectiveEnd))
                    .sorted(Comparator.comparing(CandleDTO::getDate))
                    .toList();

            int expected = expectedTradableCountInRange(effectiveStart, effectiveEnd);

            boolean partial = !effectiveStart.equals(bucketStart) || !effectiveEnd.equals(bucketEnd);
            if (candlesInBucket.size() < expected) {
                log.warn("❌ Bougie ignorée (monthly{}) {} : données incomplètes ({}/{})",
                        partial ? " PARTIAL" : "",
                        bucketStart, candlesInBucket.size(), expected);
                continue;
            }

            aggregated.add(aggregateBucket(candlesInBucket, bucketStart, "monthly"));
        }

        aggregated.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("✅ Agrégation complétée. {} bougies créées sur le timeframe monthly", aggregated.size());
        return aggregated;
    }

    private List<CandleDTO> aggregateWeeklyCandles(List<CandleDTO> m1Candles) {

        if (m1Candles == null || m1Candles.isEmpty()) {
            log.warn("⚠️ Liste vide, aucune agrégation weekly possible");
            return Collections.emptyList();
        }

        // Fenêtre globale
        LocalDateTime windowStart = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo).orElseThrow()
                .withSecond(0).withNano(0);

        LocalDateTime windowEndExclusive = m1Candles.stream()
                .map(CandleDTO::getDate).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo).orElseThrow()
                .plusMinutes(1).withSecond(0).withNano(0);

        // Tri
        List<CandleDTO> sorted = new ArrayList<>(m1Candles);
        sorted.sort(Comparator.comparing(CandleDTO::getDate));

        // bucketStart (dim 17:00 CT) → bougies
        Map<LocalDateTime, List<CandleDTO>> grouped = new HashMap<>();
        for (CandleDTO c : sorted) {
            LocalDateTime t = c.getDate().withSecond(0).withNano(0);
            LocalDateTime anchor = cmeWeekAnchorStartUtc(t);
            grouped.computeIfAbsent(anchor, k -> new ArrayList<>()).add(c);
        }

        List<CandleDTO> aggregated = new ArrayList<>();
        int ignored = 0;

        for (Map.Entry<LocalDateTime, List<CandleDTO>> e : grouped.entrySet()) {
            LocalDateTime bucketStart = e.getKey();
            LocalDateTime bucketEnd   = cmeNextWeekAnchorStartUtc(bucketStart);

            // borne par la fenêtre globale
            if (!bucketEnd.isAfter(windowStart) || !bucketStart.isBefore(windowEndExclusive)) continue;

            List<CandleDTO> candlesInBucket = e.getValue().stream()
                    .filter(c -> !c.getDate().isBefore(bucketStart) && c.getDate().isBefore(bucketEnd))
                    .sorted(Comparator.comparing(CandleDTO::getDate))
                    .toList();

            int expected = expectedTradableCountInRange(bucketStart, bucketEnd);
            if (candlesInBucket.size() < expected) {
                ignored++;
                log.warn("❌ Bougie ignorée (weekly) {} : données incomplètes ({}/{})",
                        bucketStart, candlesInBucket.size(), expected);
                continue;
            }

            aggregated.add(aggregateBucket(candlesInBucket, bucketStart, "weekly"));
        }

        aggregated.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("✅ Agrégation complétée. {} bougies créées sur le timeframe weekly", aggregated.size());
        if (ignored > 0) {
            log.info("⚠️ Agrégation weekly ignorée pour {} buckets incomplets", ignored);
        }
        return aggregated;
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

        } else if (tfMinutes == 10080) { // weekly
            ZonedDateTime chi = dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
            ZonedDateTime startChi = chi.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                    .withHour(17).withMinute(0).withSecond(0).withNano(0);
            return startChi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        } else if (tfMinutes == 43200) { // monthly (~30j)
            return getMonthlyBucketStartUtc(dateTime);
        } else {
            log.warn("⚠️ Timeframe non standard '{} minutes'. Fallback à date brute", tfMinutes);
            return dateTime.withSecond(0).withNano(0);
        }
    }



    private CandleDTO aggregateBucket(List<CandleDTO> candles, LocalDateTime bucketStart, String timeframe) {

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Le groupe de candles est vide pour l'agrégation !");
        }
        List<CandleDTO> list = new ArrayList<>(candles);
        list.sort(Comparator.comparing(CandleDTO::getDate, Comparator.nullsLast(Comparator.naturalOrder())));

        BigDecimal open = list.get(0).getOpen();
        BigDecimal close = list.get(list.size() - 1).getClose();

        BigDecimal high = list.stream()
                .map(CandleDTO::getHigh)
                .max(Comparator.naturalOrder())
                .orElse(open);

        BigDecimal low = list.stream()
                .map(CandleDTO::getLow)
                .min(Comparator.naturalOrder())
                .orElse(open);

        BigDecimal totalVolume = list.stream()
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
                .symbol(list.get(0).getSymbol()) // prend le premier, car on est déjà dans le même symbol
                .build();
    }

    private int expectedTradableCountInBucket(LocalDateTime bucketStartUtc, int tfMinutes) {
        LocalDateTime endUtc = bucketStartUtc.plusMinutes(tfMinutes);
        int count = 0;
        ZonedDateTime z = bucketStartUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        ZonedDateTime zEnd = endUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        while (z.isBefore(zEnd)) {
            ZonedDateTime zChi = z.withZoneSameInstant(EXCHANGE_ZONE);
            if (isTradableMinuteCME(zChi)) count++;
            z = z.plusMinutes(1);
        }
        return count;
    }
}
