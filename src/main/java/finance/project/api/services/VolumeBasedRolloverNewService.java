package finance.project.api.services;
import finance.project.api.model.CandleDTO;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

/**
 * Dynamic rollover builder for futures minute candles.
 * - Splits the time range into trading sessions of 23h, whose local start is 22h or 23h (Europe/Paris) depending on DST.
 * - For each session, selects the dominant contract (most present minutes; if >=90% each for several, picks max volume).
 * - Fallback fills missing minutes from secondary contracts adjusted by latest spread to dominant.
 * - If nothing exists for a tradable minute, creates a SYNTHETIC_EMPTY candle using linear interpolation
 *   between previous close and next available open (if any; else flat from previous close).
 * - When the dominant changes between sessions, applies a constant roll bridge offset computed from the last available
 *   spread between the previous dominant and the new one, to keep the synthetic series smooth.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VolumeBasedRolloverNewService {

    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    // Exchange anchor (CME Globex for 6E): 17:00 America/Chicago daily, 23h session
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("America/Chicago");
    private static final LocalTime EXCHANGE_OPEN_LOCAL = LocalTime.of(17, 0);
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int SCALE_PRICE = 9; // adapt if needed

    /**
     * Primary entry point.
     */
    public List<CandleDTO> getDynamicRolloverCandlesSessionWithMinuteFallbackGlobalIndexed(
            List<CandleDTO> inputCandles,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays
    ) {
        Objects.requireNonNull(inputCandles, "inputCandles");
        if (startDateTime == null || endDateTime == null || !startDateTime.isBefore(endDateTime)) {
            throw new IllegalArgumentException("Invalid start/end");
        }

        // --- Pre-index inputs ---
        // Only use outrights for dominant/fallback (ignore explicit calendar spreads like 6EZ5-6EH5)
        List<CandleDTO> outrights = inputCandles.stream()
                .filter(c -> c.getSymbolFuture() != null && !c.getSymbolFuture().contains("-"))
                .sorted(Comparator.comparing(CandleDTO::getDate))
                .toList();

        // Build a data-driven tradable-minute mask (per local minute-of-day) to avoid synthesizing weekends / maintenance
        // (mask removed) treat all session minutes as tradable

        // Index by (contract -> time -> candle)
        Map<String, NavigableMap<LocalDateTime, CandleDTO>> byContract = outrights.stream()
                .collect(Collectors.groupingBy(
                        CandleDTO::getSymbolFuture,
                        Collectors.toMap(
                                CandleDTO::getDate,
                                Function.identity(),
                                (a, b) -> a, // keep first if duplicates
                                TreeMap::new
                        )));

        // Index by (time -> contract -> candle) for fast lookup
        Map<LocalDateTime, Map<String, CandleDTO>> byMinuteAndContract = new HashMap<>();
        for (var e : byContract.entrySet()) {
            String cf = e.getKey();
            for (var m : e.getValue().entrySet()) {
                byMinuteAndContract.computeIfAbsent(m.getKey(), k -> new HashMap<>()).put(cf, m.getValue());
            }
        }

        // Build sessions from [start, end)
        List<SessionWindow> sessions = buildSessions(startDateTime, endDateTime, byMinuteAndContract.keySet());

        int expectedSessionMinutes = sessions.stream().mapToInt(s -> s.minutes.size()).sum();

        // Roll bridge audit
        Map<String, Integer> rollBridgeCounts = new LinkedHashMap<>();

        // Output (global indexed by minute)
        LinkedHashMap<LocalDateTime, CandleDTO> out = new LinkedHashMap<>();

        String prevSessionDominant = null;
        BigDecimal sessionOffset = ZERO; // cumulative offset applied to the whole session

        int totalSyntheticSecondary = 0;
        int totalSyntheticEmpty = 0;

        for (SessionWindow session : sessions) {
            // --- Decide dominant for this session ---
            Map<String, ContractStats> stats = computeContractStatsForSession(session, byMinuteAndContract);
            Optional<ContractStats> optDominant = selectDominant(stats, session.minutes.size());

            if (optDominant.isEmpty()) {
                log.warn("⚠️ No dominant contract found for session {} → {} (no data). Will synthesize all minutes.", session.startUtc, session.endUtc);
            }
            String dominant = optDominant.map(ContractStats::getSymbolFuture).orElse(null);

            // Roll bridge if dominant changed vs previous session
            if (dominant != null && prevSessionDominant != null && !dominant.equals(prevSessionDominant)) {
                BigDecimal bridge = estimateSpread(prevSessionDominant, dominant, session.startUtc, analysisPeriodDays, byContract);
                sessionOffset = bridge;
                String key = "SYNTHETIC_ROLL_BRIDGE_FWD:" + prevSessionDominant + "->" + dominant;
                rollBridgeCounts.merge(key, 1, Integer::sum);
                log.info("🔗 Roll bridge {} applied: offset={} (at {} UTC)", key, bridge, session.startUtc);
            } else if (dominant == null && prevSessionDominant != null) {
                // keep previous sessionOffset when no dominant exists (rare)
            }

            // Log dominant decision
            if (dominant != null) {
                int pres = stats.get(dominant).presentMinutes;
                log.info("ℹ️ Session dominant kept: {} (present={}/{})", dominant, pres, session.minutes.size());
            }

            // Pre-compute fallback order (descending presence, then volume)
            List<String> fallbackOrder = stats.values().stream()
                    .map(ContractStats::getSymbolFuture)
                    .filter(cf -> dominant == null || !cf.equals(dominant))
                    .sorted(Comparator.<String>comparingInt(cf -> stats.get(cf).presentMinutes).reversed()
                            .thenComparing(cf -> stats.get(cf).totalVolume.negate()))
                    .toList();

            // Gap buffer for SYNTHETIC_EMPTY creation
            List<LocalDateTime> gapBuffer = new ArrayList<>();

            int sessSyntheticSecondary = 0;
            int sessSkippedForSynth = 0; // minutes not filled by dominant or fallback (-> will become EMPTY)

            CandleDTO lastOut = out.isEmpty() ? null : out.get(out.keySet().stream().reduce((a, b) -> b).orElse(null));

            for (LocalDateTime ts : session.minutes) {
                CandleDTO picked = null;
                boolean fromFallback = false;

                // 1) Dominant minute
                if (dominant != null) {
                    picked = lookup(byMinuteAndContract, ts, dominant);
                }

                // 2) Fallback
                if (picked == null) {
                    for (String fb : fallbackOrder) {
                        CandleDTO sec = lookup(byMinuteAndContract, ts, fb);
                        if (sec != null) {
                            BigDecimal spread = ZERO;
                            if (dominant != null) {
                                spread = estimateSpread(dominant, fb, ts, analysisPeriodDays, byContract);
                            }
                            picked = adjustToPrimarySpace(sec, spread, "SYNTHETIC_SECONDARY_" + fb);
                            fromFallback = true;
                            break;
                        }
                    }
                }

                if (picked != null) {
                    // We reached a real candle (dominant or adjusted fallback). First, resolve any pending gaps.
                    if (!gapBuffer.isEmpty()) {
                        CandleDTO prev = lastOut;
                        BigDecimal prevClose = prev != null ? prev.getClose() : picked.getOpen();
                        BigDecimal nextOpen = picked.getOpen();
                        totalSyntheticEmpty += synthesizeGap(gapBuffer, prevClose, nextOpen, sessionOffset, out, dominant != null ? dominant : "SYNTHETIC_EMPTY");
                        sessSkippedForSynth += gapBuffer.size();
                        gapBuffer.clear();
                    }

                    // Apply session offset
                    CandleDTO adjusted = applyOffset(picked, sessionOffset);
                    out.put(ts, adjusted);
                    lastOut = adjusted;

                    if (fromFallback) sessSyntheticSecondary++;
                } else {
                    // Missing minute → buffer for SYNTHETIC_EMPTY
                    gapBuffer.add(ts);
                }
            }

            // Tail gap at session end
            if (!gapBuffer.isEmpty()) {
                CandleDTO prev = lastOut;
                BigDecimal prevClose = prev != null ? prev.getClose() : ZERO;
                // no next real candle: flat or gentle drift to prev close
                totalSyntheticEmpty += synthesizeGap(gapBuffer, prevClose, prevClose, sessionOffset, out, dominant != null ? dominant : "SYNTHETIC_EMPTY");
                sessSkippedForSynth += gapBuffer.size();
                gapBuffer.clear();
            }

            totalSyntheticSecondary += sessSyntheticSecondary;

            // Session log summary
            int presentDominant = dominant != null ? stats.get(dominant).presentMinutes : 0;
            log.info("📦 Session {} → {} | dominant={} | minutes: dominant={}, fallback={}, skippedForSynth={}",
                    session.startUtc, session.endUtc, dominant, presentDominant, sessSyntheticSecondary, sessSkippedForSynth);

            prevSessionDominant = dominant; // carry
        }

        int uniques = out.size();
        log.info("✅ Coverage sessions {} → {} : uniques={} / attendus={} ({}%)",
                startDateTime, endDateTime, uniques, expectedSessionMinutes,
                pct(uniques, expectedSessionMinutes));

        log.info("ℹ️ Attendus bruts par sessions (sans masque) = {} minutes", expectedSessionMinutes);

        log.info("🩹 Backfill M1: {} SYNTHETIC_SECONDARY, {} SYNTHETIC_EMPTY ajoutées", totalSyntheticSecondary, totalSyntheticEmpty);

        // Roll bridge audit
        if (!rollBridgeCounts.isEmpty()) {
            int sum = rollBridgeCounts.values().stream().mapToInt(i -> i).sum();
            log.info("Audit des roll bridges ({} total):", sum);
            for (var e : rollBridgeCounts.entrySet()) {
                log.info("   {} → {} occurrences", e.getKey(), e.getValue());
            }
        }

        return new ArrayList<>(out.values());
    }

    // ----------------- Helpers -----------------

    private static CandleDTO lookup(Map<LocalDateTime, Map<String, CandleDTO>> byMinuteAndContract,
                                    LocalDateTime ts, String contract) {
        Map<String, CandleDTO> m = byMinuteAndContract.get(ts);
        return (m != null) ? m.get(contract) : null;
    }

    private static CandleDTO applyOffset(CandleDTO c, BigDecimal offset) {
        if (offset == null || offset.compareTo(ZERO) == 0) return c;
        return c.toBuilder()
                .open(c.getOpen().add(offset, MC))
                .high(c.getHigh().add(offset, MC))
                .low(c.getLow().add(offset, MC))
                .close(c.getClose().add(offset, MC))
                .build();
    }

    private static CandleDTO adjustToPrimarySpace(CandleDTO sec, BigDecimal spreadPrimaryMinusSecondary, String syntheticName) {
        BigDecimal off = spreadPrimaryMinusSecondary == null ? ZERO : spreadPrimaryMinusSecondary;
        return sec.toBuilder()
                .open(sec.getOpen().add(off, MC))
                .high(sec.getHigh().add(off, MC))
                .low(sec.getLow().add(off, MC))
                .close(sec.getClose().add(off, MC))
                .symbolFuture(syntheticName)
                .build();
    }

    /**
     * Create SYNTHETIC_EMPTY candles for the buffered gap using linear interpolation between prevClose and nextOpen.
     */
    private int synthesizeGap(List<LocalDateTime> gap,
                              BigDecimal prevClose,
                              BigDecimal nextOpen,
                              BigDecimal sessionOffset,
                              LinkedHashMap<LocalDateTime, CandleDTO> out,
                              String dominantOrSynthetic) {
        if (gap.isEmpty()) return 0;
        int n = gap.size();
        BigDecimal from = prevClose == null ? ZERO : prevClose;
        BigDecimal to = (nextOpen == null ? from : nextOpen);
        BigDecimal step = n > 0 ? to.subtract(from, MC).divide(BigDecimal.valueOf(n + 1L), MC) : ZERO;

        for (int i = 0; i < n; i++) {
            LocalDateTime ts = gap.get(i);
            BigDecimal base = from.add(step.multiply(BigDecimal.valueOf(i + 1L), MC), MC);
            BigDecimal px = base.add(sessionOffset, MC);
            CandleDTO empty = CandleDTO.builder()
                    .timeframe("1m")
                    .symbol(null)
                    .symbolFuture("SYNTHETIC_EMPTY")
                    .date(ts)
                    .open(px.setScale(SCALE_PRICE, RoundingMode.HALF_UP))
                    .high(px.setScale(SCALE_PRICE, RoundingMode.HALF_UP))
                    .low(px.setScale(SCALE_PRICE, RoundingMode.HALF_UP))
                    .close(px.setScale(SCALE_PRICE, RoundingMode.HALF_UP))
                    .volume(ZERO)
                    .build();
            out.put(ts, empty);
        }
        return n;
    }

    /**
     * estimate spread(primary - secondary) at/around ts using latest known overlap within analysisPeriodDays.
     * Falls back to nearest future overlap; if nothing exists, returns ZERO.
     */
    private BigDecimal estimateSpread(String primary,
                                      String secondary,
                                      LocalDateTime ts,
                                      int analysisPeriodDays,
                                      Map<String, NavigableMap<LocalDateTime, CandleDTO>> byContract) {
        if (primary == null || secondary == null || primary.equals(secondary)) return ZERO;
        NavigableMap<LocalDateTime, CandleDTO> p = byContract.get(primary);
        NavigableMap<LocalDateTime, CandleDTO> s = byContract.get(secondary);
        if (p == null || s == null) return ZERO;

        LocalDateTime minT = ts.minusDays(Math.max(1, analysisPeriodDays));
        LocalDateTime maxT = ts.plusDays(Math.max(1, analysisPeriodDays));

        // Prefer last <= ts where both exist
        Map.Entry<LocalDateTime, CandleDTO> pe = p.floorEntry(ts);
        while (pe != null && pe.getKey().isAfter(minT)) {
            CandleDTO se = s.get(pe.getKey());
            if (se != null) return pe.getValue().getClose().subtract(se.getClose(), MC);
            pe = p.lowerEntry(pe.getKey());
        }
        // Else try first >= ts where both exist
        pe = p.ceilingEntry(ts);
        while (pe != null && pe.getKey().isBefore(maxT)) {
            CandleDTO se = s.get(pe.getKey());
            if (se != null) return pe.getValue().getClose().subtract(se.getClose(), MC);
            pe = p.higherEntry(pe.getKey());
        }
        return ZERO;
    }

    private Map<String, ContractStats> computeContractStatsForSession(SessionWindow session,
                                                                      Map<LocalDateTime, Map<String, CandleDTO>> byMinuteAndContract) {
        Map<String, ContractStats> stats = new HashMap<>();
        for (LocalDateTime ts : session.minutes) {
            Map<String, CandleDTO> m = byMinuteAndContract.get(ts);
            if (m == null) continue;
            for (var e : m.entrySet()) {
                String cf = e.getKey();
                // ignore calendar spreads in stats
                if (cf.contains("-")) continue;
                CandleDTO c = e.getValue();
                ContractStats st = stats.computeIfAbsent(cf, k -> new ContractStats(cf));
                st.presentMinutes++;
                BigDecimal v = Optional.ofNullable(c.getVolume()).orElse(ZERO);
                st.totalVolume = st.totalVolume.add(v, MC);
            }
        }
        return stats;
    }

    private Optional<ContractStats> selectDominant(Map<String, ContractStats> stats, int sessionMinutes) {
        if (stats.isEmpty()) return Optional.empty();
        // candidates with >= 90% coverage
        List<ContractStats> highCov = stats.values().stream()
                .filter(s -> s.presentMinutes >= (int) Math.floor(sessionMinutes * 0.90))
                .sorted(Comparator.comparingInt(ContractStats::getPresentMinutes).reversed()
                        .thenComparing((ContractStats s) -> s.totalVolume, Comparator.reverseOrder()))
                .toList();
        if (!highCov.isEmpty()) return Optional.of(highCov.get(0));

        // otherwise, take the most present; on tie, max volume
        return stats.values().stream()
                .sorted(Comparator.comparingInt(ContractStats::getPresentMinutes).reversed()
                        .thenComparing((ContractStats s) -> s.totalVolume, Comparator.reverseOrder()))
                .findFirst();
    }

    private static String pct(int uniques, int expected) {
        if (expected == 0) return "0.00%";
        BigDecimal p = BigDecimal.valueOf(uniques).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(expected), 2, RoundingMode.HALF_UP);
        return p + "%";
    }

    // --- Session building (23h sessions anchored at 17:00 America/Chicago → 22:00/23:00 UTC depending on US DST; skip Saturdays) ---

    private List<SessionWindow> buildSessions(LocalDateTime startUtc, LocalDateTime endUtc, Collection<LocalDateTime> knownMinutesUtc) {
        // Build sessions anchored to the EXCHANGE_ZONE open time (America/Chicago 17:00)
        // This guarantees UTC starts switch correctly at US DST boundaries (e.g., 23:00 UTC before Mar 9 2025, 22:00 UTC after).
        ZonedDateTime zStartUtc = startUtc.atZone(ZoneOffset.UTC);
        ZonedDateTime zEndUtc = endUtc.atZone(ZoneOffset.UTC);

        // Iterate over exchange-local calendar days covering the UTC range
        ZonedDateTime cursor = zStartUtc.withZoneSameInstant(EXCHANGE_ZONE).toLocalDate().atTime(EXCHANGE_OPEN_LOCAL).atZone(EXCHANGE_ZONE);
        // Ensure cursor <= end in exchange zone
        if (cursor.withZoneSameInstant(ZoneOffset.UTC).isAfter(zStartUtc)) {
            // keep
        } else {
            // if open time in exchange zone before startUtc, start from next day
            cursor = cursor.plusDays(0);
        }

        // Back up one day to include session that may start before startUtc but overlap it
        cursor = cursor.minusDays(1);

        List<SessionWindow> sessions = new ArrayList<>();
        while (true) {
            DayOfWeek dow = cursor.getDayOfWeek();
            // Start sessions only on Sun–Thu (exchange-local). Skip Friday & Saturday opens entirely
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.FRIDAY) {
                ZonedDateTime sEx = cursor;
                ZonedDateTime eEx = sEx.plusHours(23);
                ZonedDateTime sUtcZ = sEx.withZoneSameInstant(ZoneOffset.UTC);
                ZonedDateTime eUtcZ = eEx.withZoneSameInstant(ZoneOffset.UTC);

                // Intersect with requested range
                ZonedDateTime s = sUtcZ.isBefore(zStartUtc) ? zStartUtc : sUtcZ;
                ZonedDateTime e = eUtcZ.isAfter(zEndUtc) ? zEndUtc : eUtcZ;

                if (s.isBefore(e)) {
                    List<LocalDateTime> minutes = new ArrayList<>();
                    LocalDateTime t = s.toLocalDateTime();
                    while (t.isBefore(e.toLocalDateTime())) {
                        minutes.add(t);
                        t = t.plusMinutes(1);
                    }
                    sessions.add(new SessionWindow(sUtcZ.toLocalDateTime(), eUtcZ.toLocalDateTime(), minutes));
                }
            }
            // advance
            cursor = cursor.plusDays(1);
            if (cursor.withZoneSameInstant(ZoneOffset.UTC).isAfter(zEndUtc.plusDays(1))) break;
        }
        return sessions;
    }

    // --- DTOs / helpers ---

    @Data
    private static class SessionWindow {
        final LocalDateTime startUtc;
        final LocalDateTime endUtc;
        final List<LocalDateTime> minutes; // full 23h cadence (1380 normally)
    }

    @Data
    private static class ContractStats {
        final String symbolFuture;
        int presentMinutes = 0;
        BigDecimal totalVolume = ZERO;
    }
}

