package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.repositories.CandleRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolumeBasedRolloverService {

    private final CandleRepository candleRepository;

    private static final LocalTime SESSION_START_CT = LocalTime.of(17, 0);

    private enum AdjustType { DIFFERENTIAL, RATIO }
    private static final AdjustType DEFAULT_ADJUST = AdjustType.DIFFERENTIAL;
    private static final int OVERLAP_MINUTES = 10;

    private static final boolean RELAX_PREV_NEXT = true;
    private static final BigDecimal MAX_BRIDGE_ABS = new BigDecimal("0.0200");

    private static final double BRIDGE_RANGE_FACTOR = 1.25;       // gap > 1.25 * range secondaire → trop grand
    private static final BigDecimal RANGE_EPSILON   = new BigDecimal("0.00000001");

    static final class CandleIndex {
        final NavigableMap<LocalDateTime, List<CandleDTO>> byDate = new TreeMap<>();
        final Map<String, NavigableMap<LocalDateTime, List<CandleDTO>>> bySymbolThenDate = new HashMap<>();
    }

    private static final String SYNTHETIC_SECONDARY = "SYNTHETIC_SECONDARY";
    private static final String SYNTHETIC_EMPTY     = "SYNTHETIC_EMPTY";

    private ZonedDateTime toChi(LocalDateTime utc) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(EXCHANGE_ZONE);
    }
    private LocalDateTime toUtc(ZonedDateTime chi) {
        return chi.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static final double MIN_DOM_PRESENCE_RATIO = 0.40; // ex: ≥40% des minutes de la session
    private static final int    MIN_DOM_PRESENCE_ABS   = 300;  // ou au moins 300 minutes

    private int sessionPresenceCount(String sym,
                                     NavigableSet<LocalDateTime> expectedSession,
                                     Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute) {
        Map<LocalDateTime, CandleDTO> mm = bySymbolMinute.get(sym);
        if (mm == null) return 0;
        int cnt = 0;
        for (LocalDateTime m : expectedSession) if (mm.containsKey(m)) cnt++;
        return cnt;
    }

    private String pickSessionDominantWithPresence(LinkedHashSet<String> order,
                                                   NavigableSet<LocalDateTime> expectedSession,
                                                   Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute) {
        int sessionLen = expectedSession.size();
        String proposed = order.iterator().next();

        String best = null; int bestCnt = -1;
        Map<String,Integer> dbg = new LinkedHashMap<>();
        for (String s : order) {
            int cnt = sessionPresenceCount(s, expectedSession, bySymbolMinute);
            dbg.put(s, cnt);
            if (cnt > bestCnt) { best = s; bestCnt = cnt; }
        }

        int thresh = Math.max(MIN_DOM_PRESENCE_ABS, (int)Math.round(MIN_DOM_PRESENCE_RATIO * sessionLen));
        String chosen = proposed;

        int proposedCnt = dbg.getOrDefault(proposed, 0);
        if (proposedCnt == 0 && bestCnt > 0) {
            chosen = best;
        } else if (proposedCnt < thresh && bestCnt >= thresh) {
            chosen = best;
        }

        if (!Objects.equals(chosen, proposed)) {
            log.warn("🎯 Session dominant override: proposed={}({}) → chosen={}({}) / session={}",
                    proposed, proposedCnt, chosen, bestCnt, sessionLen);
        } else {
            log.info("ℹ️ Session dominant kept: {} (present={}/{})", proposed, proposedCnt, sessionLen);
        }
        // petit log des 3 premiers pour l’audit
        dbg.entrySet().stream().sorted((a,b)->Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .forEach(e -> log.debug("   presence {} = {}", e.getKey(), e.getValue()));

        return chosen;
    }

    private LocalDateTime sessionStartOnOrBefore(LocalDateTime utc) {
        ZonedDateTime chiNow = toChi(utc);
        LocalDate chiDate = chiNow.toLocalDate();
        ZonedDateTime candidate = ZonedDateTime.of(chiDate, SESSION_START_CT, EXCHANGE_ZONE)
                .withSecond(0).withNano(0);
        if (chiNow.isBefore(candidate)) {
            candidate = candidate.minusDays(1);
        }
        return toUtc(candidate);
    }

    private CandleDTO buildSyntheticSecondary(LocalDateTime minute,
                                              CandleDTO prev, CandleDTO next,
                                              CandleDTO adjustedSecondary,
                                              String timeframe,
                                              SymbolDTO baseSymbol) {

        BigDecimal open  = prev.getClose();
        BigDecimal close = next.getOpen();

        BigDecimal high  = adjustedSecondary.getHigh();
        BigDecimal low   = adjustedSecondary.getLow();

        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);
        if (high.compareTo(hiBase) < 0) high = hiBase;
        if (low.compareTo(loBase) > 0)  low  = loBase;

        String src = norm(adjustedSecondary.getSymbolFuture()); // <-- contrat source
        String tag = (src == null || src.isBlank())
                ? SYNTHETIC_SECONDARY
                : SYNTHETIC_SECONDARY + "@" + src;              // <-- PAS de '-'

        return CandleDTO.builder()
                .date(minute)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(tag)
                .build();
    }

    private boolean bridgeTooBig(BigDecimal prevClose, BigDecimal nextOpen,
                                 BigDecimal secHigh, BigDecimal secLow) {
        if (prevClose == null || nextOpen == null || secHigh == null || secLow == null) {
            return false; // pas assez d’info → ne pas bloquer
        }
        BigDecimal gap = nextOpen.subtract(prevClose).abs();
        BigDecimal range = secHigh.subtract(secLow).abs();

        if (range.compareTo(RANGE_EPSILON) < 0) {
            // secondary quasi plate → si gap non nul, considère trop grand
            return gap.compareTo(RANGE_EPSILON) > 0;
        }
        BigDecimal limit = range.multiply(BigDecimal.valueOf(BRIDGE_RANGE_FACTOR));
        return gap.compareTo(limit) > 0;
    }

    private Map<String, Map<LocalDateTime, CandleDTO>> mapBySymbolMinute(List<CandleDTO> pool) {
        Map<String, Map<LocalDateTime, CandleDTO>> res = new HashMap<>();
        for (CandleDTO c : pool) {
            if (c == null || c.getDate() == null) continue;
            String s = norm(c.getSymbolFuture());
            if (s == null || s.isBlank() || s.contains("-")) continue;
            res.computeIfAbsent(s, k -> new HashMap<>())
                    .put(floorToMinute(c.getDate()), c);
        }
        return res;
    }

    private static class AdjustCoeffs {
        final double A;  // diff médian
        final double R;  // ratio médian
        final AdjustType type;
        AdjustCoeffs(double A, double R, AdjustType type) { this.A = A; this.R = R; this.type = type; }
    }

    private AdjustCoeffs computeLocalAdjust(String frontSym,
                                            String secSym,
                                            LocalDateTime center,
                                            Map<String, Map<LocalDateTime, CandleDTO>> bySymMin,
                                            int overlapMin,
                                            AdjustType prefer) {
        Map<LocalDateTime, CandleDTO> fMap = bySymMin.get(frontSym);
        Map<LocalDateTime, CandleDTO> sMap = bySymMin.get(secSym);
        if (fMap == null || sMap == null) return new AdjustCoeffs(0.0, 1.0, prefer);

        LocalDateTime start = center.minusMinutes(overlapMin);
        LocalDateTime end   = center.plusMinutes(overlapMin);

        List<Double> diffs = new ArrayList<>();
        List<Double> ratios = new ArrayList<>();

        LocalDateTime t = floorToMinute(start);
        while (!t.isAfter(end)) {
            CandleDTO fc = fMap.get(t);
            CandleDTO sc = sMap.get(t);
            if (fc != null && sc != null && fc.getClose() != null && sc.getClose() != null) {
                double f = fc.getClose().doubleValue();
                double s = sc.getClose().doubleValue();
                if (s != 0.0) {
                    diffs.add(f - s);
                    ratios.add(f / s);
                }
            }
            t = t.plusMinutes(1);
        }

        double A = median(diffs);
        double R = median(ratios);
        return new AdjustCoeffs(A, R, prefer);
    }

    private double median(List<Double> vals) {
        if (vals == null || vals.isEmpty()) return 0.0;
        double[] arr = vals.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int n = arr.length;
        return (n % 2 == 1) ? arr[n/2] : (arr[n/2 - 1] + arr[n/2]) / 2.0;
    }

    private CandleDTO applyAdjust(CandleDTO sec, AdjustCoeffs adj) {
        if (sec == null || sec.getOpen()==null || sec.getHigh()==null || sec.getLow()==null || sec.getClose()==null) return sec;

        switch (adj.type) {
            case DIFFERENTIAL -> {
                BigDecimal A = BigDecimal.valueOf(adj.A);
                return sec.toBuilder()
                        .open(sec.getOpen().add(A))
                        .high(sec.getHigh().add(A))
                        .low(sec.getLow().add(A))
                        .close(sec.getClose().add(A))
                        .build();
            }
            case RATIO -> {
                BigDecimal R = BigDecimal.valueOf(adj.R);
                return sec.toBuilder()
                        .open(sec.getOpen().multiply(R))
                        .high(sec.getHigh().multiply(R))
                        .low(sec.getLow().multiply(R))
                        .close(sec.getClose().multiply(R))
                        .build();
            }
            default -> { return sec; }
        }
    }



    /** Prochain début de session (17:00 CT) après un début de session donné */
    private LocalDateTime nextSessionStart(LocalDateTime utcSessionStart) {
        ZonedDateTime chi = toChi(utcSessionStart);
        LocalDate nextDate = chi.toLocalDate().plusDays(1);
        ZonedDateTime nextChi = ZonedDateTime.of(nextDate, SESSION_START_CT, EXCHANGE_ZONE)
                .withSecond(0).withNano(0);
        return toUtc(nextChi);
    }

    /** Le pivot de session (17:00 CT) est-il une minute “tradable” selon tes règles ? */
    private boolean isTradableSessionStart(LocalDateTime utcSessionStart) {
        return isTradableMinuteCME(toChi(utcSessionStart));
    }

    private static String norm(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * Construit un mapping minute -> meilleure "bougie secondaire" (autre contrat),
     * en choisissant celle avec le volume max à cette minute (spreads ignorés).
     */
    private Map<LocalDateTime, CandleDTO> buildBestSecondaryByMinute(List<CandleDTO> pool) {

        Map<LocalDateTime, CandleDTO> best = new HashMap<>();
        for (CandleDTO c : pool) {

            String sf = norm(c.getSymbolFuture());
            if (sf == null || sf.isBlank() || sf.contains("-")) continue;
            LocalDateTime m = floorToMinute(c.getDate());
            CandleDTO cur = best.get(m);
            if (cur == null) {
                best.put(m, c);
            } else {
                BigDecimal vNew = c.getVolume() == null ? BigDecimal.ZERO : c.getVolume();
                BigDecimal vCur = cur.getVolume() == null ? BigDecimal.ZERO : cur.getVolume();
                if (vNew.compareTo(vCur) > 0) best.put(m, c);
            }
        }
        return best;
    }

    private static final int MAX_BACKFILL_PASSES = 3;
    // Option: garde-fou sur la taille du pont (ex: 15 pips EURUSD)
    private static final BigDecimal MAX_BRIDGE = new BigDecimal("0.0015");

    private CandleDTO addSpread(CandleDTO base, CandleDTO s) {
        return base.toBuilder()
                .open(base.getOpen().add(s.getOpen()))
                .high(base.getHigh().add(s.getHigh()))
                .low (base.getLow ().add(s.getLow ()))
                .close(base.getClose().add(s.getClose()))
                .build();
    }

    private CandleDTO negateSpread(CandleDTO s) {
        return s.toBuilder()
                .open (s.getOpen ().negate())
                .high (s.getHigh ().negate())
                .low  (s.getLow  ().negate())
                .close(s.getClose().negate())
                .build();
    }

    public List<CandleDTO> backfillOneMinuteGapsWithSynthetic(
            List<CandleDTO> rolled,
            List<CandleDTO> pool,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive) {

        if (rolled == null || rolled.isEmpty()) return rolled;

        // index série sortie
        Map<LocalDateTime, CandleDTO> byMinute = new HashMap<>();
        for (CandleDTO c : rolled) {
            if (c == null || c.getDate() == null) continue;
            byMinute.put(floorToMinute(c.getDate()), c);
        }

        // pool: meilleurs outrights à la minute (volume max)
        Map<String, Map<LocalDateTime, CandleDTO>> bySymMin = mapBySymbolMinute(pool);  // tu l'as déjà
        Map<LocalDateTime, CandleDTO> secondaryAtMinute = new HashMap<>();
        // construit le "meilleur" sur tous outrights
        for (LocalDateTime m : expectedTradableMinutes(floorToMinute(startInclusive), floorToMinute(endExclusive))) {
            CandleDTO pick = pickBestOutrightAtMinute(bySymMin, m);
            if (pick != null) secondaryAtMinute.put(m, pick);
        }

        Set<LocalDateTime> expected = expectedTradableMinutes(
                floorToMinute(startInclusive), floorToMinute(endExclusive));

        String timeframe = Optional.ofNullable(rolled.get(0).getTimeframe()).orElse("1min");
        SymbolDTO baseSymbol = rolled.get(0).getSymbol();

        List<CandleDTO> patched = new ArrayList<>(rolled);
        int addedSecondary = 0, addedEmpty = 0;

        for (LocalDateTime m : expected) {
            if (byMinute.containsKey(m)) continue; // pas de trou

            // voisins
            LocalDateTime prevMin = m.minusMinutes(1);
            LocalDateTime nextMin = m.plusMinutes(1);
            CandleDTO prev = byMinute.get(prevMin);
            CandleDTO next = byMinute.get(nextMin);
            boolean havePrev = (prev != null);
            boolean haveNext = (next != null);

            if ((!havePrev || !haveNext) && RELAX_PREV_NEXT) {
                for (int k = 2; k <= 5 && (!havePrev || !haveNext); k++) {
                    if (!havePrev) { prev = byMinute.get(m.minusMinutes(k)); havePrev = (prev != null); }
                    if (!haveNext) { next = byMinute.get(m.plusMinutes(k)); haveNext = (next != null); }
                }
            }

            CandleDTO sec = secondaryAtMinute.get(m);

            // DEBUG
            if (log.isDebugEnabled()) {
                log.debug("🔎 MISSING {} | prev={} next={} | sec={}",
                        m, havePrev ? prev.getSymbolFuture() : "∅",
                        haveNext ? next.getSymbolFuture() : "∅",
                        (sec != null ? sec.getSymbolFuture() : "∅"));
            }

            CandleDTO synth;

            if (sec != null) {
                // Ajust local vers le "front" (prend le voisin dispo en priorité)
                String frontSym = canon(havePrev ? prev.getSymbolFuture() : haveNext ? next.getSymbolFuture() : sec.getSymbolFuture());
                String secSym   = canon(sec.getSymbolFuture());

                AdjustCoeffs adj = (frontSym != null && frontSym.equals(secSym))
                        ? new AdjustCoeffs(0.0, 1.0, DEFAULT_ADJUST)
                        : computeLocalAdjust(frontSym, secSym, m, mapBySymbolMinute(pool), OVERLAP_MINUTES, DEFAULT_ADJUST);

                CandleDTO secAdj = applyAdjust(sec, adj);

                if (havePrev && haveNext) {
                    CandleDTO built = buildSyntheticSecondary(m, prev, next, secAdj, timeframe, baseSymbol)
                            .toBuilder()
                            .symbolFuture(SYNTHETIC_SECONDARY + ":" + secSym)
                            .build();
                    synth = built;
                    addedSecondary++;
                } else if (havePrev) {
                    CandleDTO built = buildSyntheticSecondaryOneSided(m, prev, true, secAdj, timeframe, baseSymbol)
                            .toBuilder()
                            .symbolFuture(SYNTHETIC_SECONDARY + ":" + secSym)
                            .build();
                    synth = built;
                    addedSecondary++;
                } else if (haveNext) {
                    CandleDTO built = buildSyntheticSecondaryOneSided(m, next, false, secAdj, timeframe, baseSymbol)
                            .toBuilder()
                            .symbolFuture(SYNTHETIC_SECONDARY + ":" + secSym)
                            .build();
                    synth = built;
                    addedSecondary++;
                } else {
                    // aucun voisin → EMPTY plano
                    synth = buildSyntheticEmpty(m, null, null, timeframe, baseSymbol);
                    addedEmpty++;
                }
            } else {
                // pas d'info secondaire → EMPTY avec voisins si dispo
                if (havePrev || haveNext) {
                    CandleDTO side = havePrev ? prev : next;
                    BigDecimal oc = havePrev ? side.getClose() : side.getOpen();
                    synth = CandleDTO.builder()
                            .date(m).open(oc).close(oc).high(oc).low(oc)
                            .volume(BigDecimal.ZERO)
                            .timeframe(timeframe)
                            .symbol(baseSymbol)
                            .symbolFuture(SYNTHETIC_EMPTY)
                            .build();
                } else {
                    synth = buildSyntheticEmpty(m, null, null, timeframe, baseSymbol);
                }
                addedEmpty++;
            }

            patched.add(synth);
            byMinute.put(m, synth);
        }

        patched.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("🩹 Backfill M1: {} SYNTHETIC_SECONDARY, {} SYNTHETIC_EMPTY ajoutées", addedSecondary, addedEmpty);
        return patched;
    }

    public List<CandleDTO> getDynamicRolloverCandlesSessionWithMinuteFallbackGlobalIndexed(
            List<CandleDTO> inputCandles,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays
    ) {
        if (inputCandles == null || inputCandles.isEmpty()) {
            log.warn("⚠️ Liste source vide → rien à faire.");
            return Collections.emptyList();
        }

        // 1) Nettoyage minimal
        List<CandleDTO> src = inputCandles.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getDate() != null)
                .filter(c -> c.getSymbolFuture() != null && !c.getSymbolFuture().isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
        if (src.isEmpty()) {
            log.warn("⚠️ Après filtrage, plus aucune bougie exploitable.");
            return Collections.emptyList();
        }

        // 2) Index temporel (fenêtres) + volumes globaux
        CandleIndex idx = buildIndex(src);

        Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute = new HashMap<>();
        Map<String, Double> globalVol = new HashMap<>();
        for (CandleDTO c : src) {
            String sym = c.getSymbolFuture();
            LocalDateTime m = floorToMinute(c.getDate());
            bySymbolMinute.computeIfAbsent(sym, k -> new HashMap<>()).put(m, c);
            double v = (c.getVolume() != null) ? c.getVolume().doubleValue() : 0.0;
            // on cumule le volume seulement pour les outrights
            if (!sym.contains("-")) globalVol.merge(canon(sym), v, Double::sum);
        }

        // 3) Index spreads: "A-B" → minute → spread candle
        Map<String, Map<LocalDateTime, CandleDTO>> bySpreadMinute = new HashMap<>();
        for (CandleDTO c : src) {
            String s = canon(c.getSymbolFuture());
            if (s == null || !s.contains("-")) continue; // garder UNIQUEMENT les spreads ici
            LocalDateTime m = floorToMinute(c.getDate());
            bySpreadMinute.computeIfAbsent(s, k -> new HashMap<>()).put(m, c);
        }

        // 4) Ranking GLOBAL par volume
        List<String> globalRanking = globalVol.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        // 5) Minutes attendues globales
        NavigableSet<LocalDateTime> expectedAll = new TreeSet<>(
                expectedTradableMinutes(
                        startDateTime.withSecond(0).withNano(0),
                        endDateTime.withSecond(0).withNano(0)
                )
        );

        // helper: ranking session [a,b)
        java.util.function.BiFunction<LocalDateTime, LocalDateTime, List<String>> rankSymbolsByVolume =
                (a, b) -> {
                    List<CandleDTO> window = getWindow(idx, a, b);
                    if (window.isEmpty()) return List.of();
                    return window.stream()
                            .filter(c -> !c.getSymbolFuture().contains("-"))
                            .collect(Collectors.groupingBy(
                                    c -> canon(c.getSymbolFuture()),
                                    Collectors.summingDouble(c -> c.getVolume() != null ? c.getVolume().doubleValue() : 0.0)
                            ))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .map(Map.Entry::getKey)
                            .toList();
                };

        List<CandleDTO> out = new ArrayList<>();
        LocalDateTime sessionStart = sessionStartOnOrBefore(startDateTime);

        // état pour bridging “runs”
        BigDecimal lastCloseOut = null;
        String    activeRunSym  = null;
        BigDecimal runOffset    = BigDecimal.ZERO;

        while (sessionStart.isBefore(endDateTime)) {
            LocalDateTime sessionNext = nextSessionStart(sessionStart);

            LocalDateTime windowStart = sessionStart.isBefore(startDateTime) ? startDateTime : sessionStart;
            LocalDateTime windowEnd   = sessionNext.isAfter(endDateTime)     ? endDateTime   : sessionNext;
            if (!windowStart.isBefore(windowEnd)) break;

            if (!isTradableSessionStart(sessionStart)) {
                sessionStart = sessionNext;
                continue;
            }

            LocalDateTime analysisEnd   = sessionStart;
            LocalDateTime analysisStart = toUtc(toChi(sessionStart).minusDays(analysisPeriodDays));
            List<String> sessionRanking = rankSymbolsByVolume.apply(analysisStart, analysisEnd);

            List<String> orderList = new ArrayList<>();
            if (!sessionRanking.isEmpty()) orderList.addAll(sessionRanking);
            if (orderList.isEmpty()) orderList.addAll(globalRanking);
            if (orderList.isEmpty()) {
                sessionStart = sessionNext;
                continue;
            }

            String dominant = orderList.get(0); // canon déjà
            LinkedHashSet<String> order = new LinkedHashSet<>(orderList);

            NavigableSet<LocalDateTime> expectedSession = expectedAll.subSet(
                    windowStart.withSecond(0).withNano(0), true,
                    windowEnd.withSecond(0).withNano(0), false
            );

            String sessionDominant = pickSessionDominantWithPresence(order, expectedSession, bySymbolMinute);
            if (!sessionDominant.equals(dominant)) {
                // place le dominant retenu en tête de l’ordre
                LinkedHashSet<String> newOrder = new LinkedHashSet<>();
                newOrder.add(sessionDominant);
                for (String s : order) if (!s.equals(sessionDominant)) newOrder.add(s);
                order = newOrder;
                dominant = sessionDominant;
            }

            int usedDom = 0, usedFb = 0, skipped = 0;

            for (LocalDateTime m : expectedSession) {
                CandleDTO chosen = null;

                Map<LocalDateTime, CandleDTO> mdom = bySymbolMinute.get(dominant);
                if (mdom != null) chosen = mdom.get(m);

                if (chosen == null) {
                    // autres symbols en fallback
                    for (String sym : order) {
                        if (sym.equals(dominant)) continue;
                        Map<LocalDateTime, CandleDTO> mm = bySymbolMinute.get(sym);
                        if (mm == null) continue;
                        CandleDTO alt = mm.get(m);
                        if (alt != null) { chosen = alt; usedFb++; break; }
                    }
                } else {
                    usedDom++;
                }

                if (chosen == null) { skipped++; continue; }

                String chosenSym = canon(chosen.getSymbolFuture());
                CandleDTO outCandle;

                if (chosenSym.equals(dominant)) {
                    // dominant : pas d’offset
                    outCandle = chosen;
                    activeRunSym = chosenSym;
                    runOffset = BigDecimal.ZERO;
                } else {
                    // 1) spread exact si dispo
                    CandleDTO adjusted = null;
                    CandleDTO sprAB = bySpreadMinute.getOrDefault(spreadKey(dominant, chosenSym), Map.of()).get(m);
                    if (sprAB != null) {
                        adjusted = translateWithSpread(chosen, sprAB, "1min", chosen.getSymbol());
                    } else {
                        CandleDTO sprBA = bySpreadMinute.getOrDefault(spreadKey(chosenSym, dominant), Map.of()).get(m);
                        if (sprBA != null) {
                            CandleDTO sprNeg = sprBA.toBuilder()
                                    .open(sprBA.getOpen().negate())
                                    .high(sprBA.getHigh().negate())
                                    .low (sprBA.getLow().negate())
                                    .close(sprBA.getClose().negate())
                                    .build();
                            adjusted = translateWithSpread(chosen, sprNeg, "1min", chosen.getSymbol());
                        }
                    }

                    // 2) sinon bridging de continuité
                    if (adjusted == null && lastCloseOut != null && chosen.getClose() != null) {
                        BigDecimal needed = lastCloseOut.subtract(chosen.getClose());
                        if (!bridgeTooBig(needed)) {
                            adjusted = shiftOHLC(chosen, needed);
                            if (!chosenSym.equals(activeRunSym)) runOffset = needed;
                        }
                    }

                    // 3) sinon runOffset courant (si on reste sur le même run)
                    if (adjusted == null && !runOffset.equals(BigDecimal.ZERO) && chosenSym.equals(activeRunSym)) {
                        adjusted = shiftOHLC(chosen, runOffset);
                    }

                    // 4) dernier recours: ajust local médian
                    if (adjusted == null) {
                        AdjustCoeffs adj = computeLocalAdjust(
                                dominant, chosenSym, m, bySymbolMinute, OVERLAP_MINUTES, DEFAULT_ADJUST
                        );
                        adjusted = applyAdjust(chosen, adj);
                    }

                    outCandle = adjusted.toBuilder()
                            .symbolFuture(SYNTHETIC_SECONDARY + ":" + chosenSym)
                            .volume(BigDecimal.ZERO)
                            .build();

                    activeRunSym = chosenSym;
                }

                out.add(outCandle);
                lastCloseOut = outCandle.getClose();
            }
            if (usedFb > usedDom) {
                log.warn("⚠️ fallback > dominant in session {} → {} (dom={} fb={}) | dominant={}",
                        sessionStart, sessionNext, usedDom, usedFb, dominant);
            }
            log.info("📦 Session {} → {} | dominant={} | minutes: dominant={}, fallback={}, skippedForSynth={}",
                    sessionStart, sessionNext, dominant, usedDom, usedFb, skipped);

            sessionStart = sessionNext;
        }

        // tri + log coverage rapide
        out.sort(Comparator.comparing(CandleDTO::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        if (!out.isEmpty()) {
            LocalDateTime s = out.get(0).getDate();
            LocalDateTime e = out.get(out.size()-1).getDate().plusMinutes(1);
            Set<LocalDateTime> expected = expectedTradableMinutes(floorToMinute(s), floorToMinute(e));
            int uniq = (int) out.stream().map(c -> c.getDate().withSecond(0).withNano(0)).distinct().count();
            double cov = expected.isEmpty() ? 100.0 : 100.0 * uniq / expected.size();
            log.info("✅ Coverage après session+fallback (avant synthèse) {} → {} : uniques={} / attendus={} ({}%)",
                    s, e, uniq, expected.size(), String.format(Locale.US, "%.2f", cov));
        }

        return out;
    }

    private static String canon(String s) {
        return s == null ? null : s.replace(" ", "").toUpperCase(Locale.ROOT);
    }
    private static String spreadKey(String a, String b) { // "A-B"
        return canon(a) + "-" + canon(b);
    }

    // --- logs rapides ---
    private static String fmt(CandleDTO c) {
        if (c == null) return "null";
        return String.format(Locale.US,
                "%s O=%s H=%s L=%s C=%s V=%s @%s",
                c.getSymbolFuture(),
                String.valueOf(c.getOpen()),
                String.valueOf(c.getHigh()),
                String.valueOf(c.getLow()),
                String.valueOf(c.getClose()),
                String.valueOf(c.getVolume()),
                String.valueOf(c.getDate()));
    }

    // --- décalage additif sur OHLC (bridging/offset) ---
    private static CandleDTO shiftOHLC(CandleDTO c, BigDecimal off) {
        if (c == null || off == null || BigDecimal.ZERO.compareTo(off) == 0) return c;
        return c.toBuilder()
                .open (c.getOpen().add(off))
                .high (c.getHigh().add(off))
                .low  (c.getLow().add(off))
                .close(c.getClose().add(off))
                .build();
    }
    private static boolean bridgeTooBig(BigDecimal off) {
        return off == null || off.abs().compareTo(MAX_BRIDGE_ABS) > 0;
    }

    // --- synthétiques simples ---
    private CandleDTO buildSyntheticEmpty(LocalDateTime minute,
                                          CandleDTO prev, CandleDTO next,
                                          String timeframe,
                                          SymbolDTO baseSymbol) {
        BigDecimal ocPrev = (prev != null && prev.getClose()!=null) ? prev.getClose() : BigDecimal.ZERO;
        BigDecimal ocNext = (next != null && next.getOpen() !=null) ? next.getOpen()  : ocPrev;

        BigDecimal open  = ocPrev;
        BigDecimal close = ocNext;
        BigDecimal high  = open.max(close);
        BigDecimal low   = open.min(close);

        return CandleDTO.builder()
                .date(minute)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_EMPTY)
                .build();
    }

    private CandleDTO buildSyntheticSecondaryOneSided(
            LocalDateTime minute,
            CandleDTO side,                // prev OU next (un seul)
            boolean isPrev,                // true si side = prev
            CandleDTO adjustedSecondary,   // sert pour les mèches
            String timeframe,
            SymbolDTO baseSymbol
    ) {
        BigDecimal oc = isPrev ? side.getClose() : side.getOpen(); // O=C
        BigDecimal open  = oc;
        BigDecimal close = oc;

        BigDecimal high = adjustedSecondary.getHigh();
        BigDecimal low  = adjustedSecondary.getLow();

        // cohérence
        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);
        if (high.compareTo(hiBase) < 0) high = hiBase;
        if (low.compareTo(loBase)  > 0) low  = loBase;

        return CandleDTO.builder()
                .date(minute)
                .open(open).close(close).high(high).low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_SECONDARY) // le call-site suffixe : ":<sym>"
                .build();
    }

    // --- application d’un spread A-B sur un outrigth B pour l’exprimer en A ---
    private CandleDTO translateWithSpread(CandleDTO secB, CandleDTO sprAminusB, String timeframe, SymbolDTO baseSymbol) {
        BigDecimal o = secB.getOpen().add(sprAminusB.getOpen());
        BigDecimal h = secB.getHigh().add(sprAminusB.getHigh());
        BigDecimal l = secB.getLow().add(sprAminusB.getLow());
        BigDecimal c = secB.getClose().add(sprAminusB.getClose());
        return secB.toBuilder()
                .open(o).high(h).low(l).close(c)
                .symbolFuture(SYNTHETIC_SECONDARY) // le call-site suffixe : ":<sym>"
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .volume(BigDecimal.ZERO)
                .build();
    }

    // --- meilleur outright à une minute (par volume) ---
    private CandleDTO pickBestOutrightAtMinute(Map<String, Map<LocalDateTime, CandleDTO>> bySymMin,
                                               LocalDateTime m) {
        CandleDTO best = null;
        BigDecimal bestVol = BigDecimal.valueOf(-1);
        for (Map.Entry<String, Map<LocalDateTime, CandleDTO>> e : bySymMin.entrySet()) {
            String sym = e.getKey();
            if (sym == null || sym.contains("-")) continue;
            CandleDTO c = e.getValue().get(m);
            if (c == null) continue;
            BigDecimal v = c.getVolume() == null ? BigDecimal.ZERO : c.getVolume();
            if (best == null || v.compareTo(bestVol) > 0) {
                best = c; bestVol = v;
            }
        }
        return best;
    }

    // --- debug : quels symbols bruts existent à une minute ---
    private Map<LocalDateTime, List<String>> buildPoolPresenceIndex(List<CandleDTO> pool) {
        Map<LocalDateTime, Set<String>> tmp = new HashMap<>();
        for (CandleDTO c : pool) {
            if (c == null || c.getDate() == null) continue;
            String s = canon(c.getSymbolFuture());
            if (s == null || s.isBlank() || s.contains("-")) continue;
            LocalDateTime m = floorToMinute(c.getDate());
            tmp.computeIfAbsent(m, k -> new LinkedHashSet<>()).add(s);
        }
        Map<LocalDateTime, List<String>> out = new HashMap<>();
        tmp.forEach((k,v)-> out.put(k, new ArrayList<>(v)));
        return out;
    }

    private CandleDTO buildSyntheticSecondaryWithSourceName(
            LocalDateTime minute,
            CandleDTO prev, CandleDTO next,
            CandleDTO adjustedSecondary,
            String timeframe,
            SymbolDTO baseSymbol,
            String sourceSym) {

        BigDecimal open  = prev.getClose();
        BigDecimal close = next.getOpen();

        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);
        BigDecimal high = adjustedSecondary.getHigh();
        BigDecimal low  = adjustedSecondary.getLow();
        if (high.compareTo(hiBase) < 0) high = hiBase;
        if (low.compareTo(loBase) > 0)  low  = loBase;

        return CandleDTO.builder()
                .date(minute).open(open).close(close)
                .high(high).low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe).symbol(baseSymbol)
                .symbolFuture("SYNTHETIC_SECONDARY(" + sourceSym + ")")
                .build();
    }

    private CandleDTO buildSyntheticEmptyWithSourceName(
            LocalDateTime minute, CandleDTO prev, CandleDTO next,
            String timeframe, SymbolDTO baseSymbol) {

        // si un seul côté dispo, O=C sur ce côté (sinon O=C=0)
        BigDecimal oc = (prev != null) ? prev.getClose()
                : (next != null) ? next.getOpen()
                : BigDecimal.ZERO;

        String src = (prev != null) ? prev.getSymbolFuture()
                : (next != null) ? next.getSymbolFuture()
                : "NONE";

        return CandleDTO.builder()
                .date(minute).open(oc).close(oc)
                .high(oc).low(oc)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe).symbol(baseSymbol)
                .symbolFuture("SYNTHETIC_EMPTY(" + src + ")")
                .build();
    }

    private static String key(String s) {
        return s == null ? null : s.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    // ===============================================
// Règles CME (FX / 6E) – fuseau Chicago
// ===============================================
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("America/Chicago");
    private static final LocalTime DAILY_BREAK_START = LocalTime.of(16, 0); // 16:00
    private static final LocalTime DAILY_BREAK_END   = LocalTime.of(17, 0); // 17:00
    private static final LocalTime SUNDAY_OPEN       = LocalTime.of(17, 0); // dim >= 17:00
    private static final LocalTime FRIDAY_CLOSE      = LocalTime.of(16, 0); // ven < 16:00

    private boolean isTradableMinuteCME(ZonedDateTime zdt) {
        DayOfWeek dow = zdt.getDayOfWeek();
        LocalTime t = zdt.toLocalTime();

        if (dow == DayOfWeek.SATURDAY) return false;
        // Break quotidien 16:00–17:00 (heures Chicago)
        if (!t.isBefore(DAILY_BREAK_START) && t.isBefore(DAILY_BREAK_END)) return false;
        // Dimanche: ouvert à partir de 17:00
        if (dow == DayOfWeek.SUNDAY) return !t.isBefore(SUNDAY_OPEN);
        // Vendredi: fermé à partir de 16:00
        if (dow == DayOfWeek.FRIDAY) return t.isBefore(FRIDAY_CLOSE);
        // Lundi–Jeudi hors break: ouvert
        return true;
    }

    // ===============================================
// Utilitaires
// ===============================================
    static LocalDateTime floorToMinute(LocalDateTime dt) {
        return dt.withSecond(0).withNano(0);
    }

    private Set<LocalDateTime> expectedTradableMinutes(LocalDateTime startInclusiveUtc,
                                                       LocalDateTime endExclusiveUtc) {
        Set<LocalDateTime> expected = new LinkedHashSet<>();

        // On parcourt en UTC
        ZonedDateTime zCurUtc = startInclusiveUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);
        ZonedDateTime zEndUtc = endExclusiveUtc.atZone(ZoneOffset.UTC).withSecond(0).withNano(0);

        while (zCurUtc.isBefore(zEndUtc)) {
            // On évalue la "tradabilité" à l'instant correspondant en heure de Chicago
            ZonedDateTime zChi = zCurUtc.withZoneSameInstant(EXCHANGE_ZONE);
            if (isTradableMinuteCME(zChi)) {
                // On stocke la minute attendue en UTC (cohérent avec CandleDTO.date)
                expected.add(zCurUtc.toLocalDateTime());
            }
            zCurUtc = zCurUtc.plusMinutes(1);
        }
        return expected;
    }


    private CandleIndex buildIndex(List<CandleDTO> input) {
        CandleIndex idx = new CandleIndex();

        for (CandleDTO c : input) {
            if (c == null || c.getDate() == null) continue;
            String sym = c.getSymbolFuture();
            if (sym == null || sym.isBlank()) continue;

            // byDate
            idx.byDate.computeIfAbsent(c.getDate(), k -> new ArrayList<>()).add(c);

            // bySymbolThenDate
            NavigableMap<LocalDateTime, List<CandleDTO>> perSymbol =
                    idx.bySymbolThenDate.computeIfAbsent(sym, k -> new TreeMap<>());
            perSymbol.computeIfAbsent(c.getDate(), k -> new ArrayList<>()).add(c);
        }

        return idx;
    }

    // Récupère toutes les candles [start, end)
    private List<CandleDTO> getWindow(CandleIndex idx, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) return Collections.emptyList();
        NavigableMap<LocalDateTime, List<CandleDTO>> sub = idx.byDate.subMap(start, true, end, false);
        if (sub.isEmpty()) return Collections.emptyList();
        List<CandleDTO> out = new ArrayList<>();
        for (List<CandleDTO> bucket : sub.values()) out.addAll(bucket);
        return out;
    }

    private CandleDTO snapIfRawBaseExists(LocalDateTime m,
                                          String baseSym,
                                          CandleDTO candidate,
                                          Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute) {
        Map<LocalDateTime, CandleDTO> map = bySymbolMinute.get(baseSym);
        if (map == null) return candidate;
        CandleDTO base = map.get(m);
        if (base == null) return candidate;

        BigDecimal o = base.getOpen();
        BigDecimal c = base.getClose();

        // high/low doivent englober O et C
        BigDecimal hi = candidate.getHigh() != null ? candidate.getHigh() : (o.max(c));
        BigDecimal lo = candidate.getLow()  != null ? candidate.getLow()  : (o.min(c));
        if (hi.compareTo(o) < 0) hi = o;
        if (hi.compareTo(c) < 0) hi = c;
        if (lo.compareTo(o) > 0) lo = o;
        if (lo.compareTo(c) > 0) lo = c;

        return candidate.toBuilder()
                .open(o)
                .close(c)
                .high(hi)
                .low(lo)
                .build();
    }

    private CandleDTO adjustToBase(
            LocalDateTime m,
            String baseSym,                // A
            CandleDTO chosen,              // B
            Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute,
            Map<String, Map<LocalDateTime, CandleDTO>> bySpreadMinute,
            int overlapMinutes
    ) {
        String A = norm(baseSym);
        String B = norm(chosen.getSymbolFuture());

        if (A.equals(B)) return chosen;    // rien à faire

        // 1) Spread direct A-B à la minute m ?
        CandleDTO sAB = bySpreadMinute.getOrDefault(spreadKey(A,B), Map.of()).get(m);
        if (sAB != null) {
            return addSpread(chosen, sAB); // P_B + (A-B)
        }

        // 2) Spread inverse B-A ?
        CandleDTO sBA = bySpreadMinute.getOrDefault(spreadKey(B,A), Map.of()).get(m);
        if (sBA != null) {
            return addSpread(chosen, negateSpread(sBA)); // P_B - (B-A)
        }

        // 3) Pas de spread dispo → fallback différentiel local (médiane sur ±overlap)
        AdjustCoeffs adj = computeLocalAdjust(A, B, m, bySymbolMinute, overlapMinutes, AdjustType.DIFFERENTIAL);
        return applyAdjust(chosen, adj);
    }

    public Map<LocalDateTime, String> getDominantContractsPerDay(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays) {

        Map<LocalDateTime, String> dominanceMap = new LinkedHashMap<>();
        LocalDateTime currentDateTime = startDateTime;

        log.info("🚀 Génération de la heatmap de dominance (DateTime) de {} à {}", startDateTime, endDateTime);

        while (!currentDateTime.isAfter(endDateTime)) {

            DayOfWeek dayOfWeek = currentDateTime.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                log.info("⏭️ Weekend ignoré : {}", currentDateTime);
                currentDateTime = currentDateTime.plusDays(1);
                continue;
            }

            // La période d'analyse se termine à currentDateTime
            LocalDateTime analysisStartDateTime = currentDateTime.minusDays(analysisPeriodDays);
            LocalDateTime analysisEndDateTime = currentDateTime;

            log.info("🔎 Analyse de dominance à {} sur les {} derniers jours ({} -> {})",
                    currentDateTime, analysisPeriodDays, analysisStartDateTime, analysisEndDateTime);

            // On récupère les candles sur la période d'analyse
            List<Candle> analysisCandles = candleRepository.findByDateBetween(
                    analysisStartDateTime, analysisEndDateTime
            );

            if (analysisCandles.isEmpty()) {
                log.warn("❌ Aucune donnée trouvée pour la fenêtre {} -> {}", analysisStartDateTime, analysisEndDateTime);
                dominanceMap.put(currentDateTime, "AUCUN");
                currentDateTime = currentDateTime.plusDays(1);
                continue;
            }

            // Calcul du volume cumulé par symbolFuture
            Map<String, Double> volumeBySymbolFuture = analysisCandles.stream()
                    .collect(Collectors.groupingBy(
                            Candle::getSymbolFuture,
                            Collectors.summingDouble(c -> c.getVolume() != null ? c.getVolume().doubleValue() : 0.0)
                    ));

            // Trouver le dominant
            Optional<Map.Entry<String, Double>> dominantEntry = volumeBySymbolFuture.entrySet().stream()
                    .max(Map.Entry.comparingByValue());

            String dominantSymbol = dominantEntry.map(Map.Entry::getKey).orElse("AUCUN");

            dominanceMap.put(currentDateTime, dominantSymbol);

            log.info("✅ Dominant sur {} : {}", currentDateTime, dominantSymbol);

            // Incrémente d'un jour (on pourrait passer à 1h si tu veux plus précis)
            currentDateTime = currentDateTime.plusDays(1);
        }

        log.info("🎉 Heatmap DateTime générée avec {} points", dominanceMap.size());

        return dominanceMap;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MinuteGap {
        private LocalDateTime minute;
        private List<String> availableSymbols; // symboles ayant une bougie brute à cette minute (hors spreads)
        private boolean presentInPool;         // true si au moins un contrat a une bougie brute à cette minute
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RolloverCoverageReport {
        private int expectedMinutes;                 // minutes “tradables” attendues
        private int producedMinutes;                 // minutes effectivement produites par ta série rollée
        private double coveragePct;                  // produced / expected
        private List<MinuteGap> missing;             // détail des minutes manquantes
    }


    public RolloverCoverageReport auditRolloverCoverage(
            List<CandleDTO> rolled,     // la série produite par ton rollover (après fallback/synthèse ou avant selon ton besoin)
            List<CandleDTO> pool,       // toutes les bougies brutes (tous contrats, spreads inclus possibles)
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) {
        // minutes attendues selon le calendrier CME (UTC)
        Set<LocalDateTime> expected = expectedTradableMinutes(
                floorToMinute(startInclusive), floorToMinute(endExclusive));
        int expectedCount = expected.size();

        // minutes produites par la série "rolled"
        Set<LocalDateTime> produced = rolled.stream()
                .filter(Objects::nonNull)
                .map(CandleDTO::getDate)
                .filter(Objects::nonNull)
                .map(VolumeBasedRolloverService::floorToMinute)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int producedCount = produced.size();

        // index brut: pour savoir si, à une minute manquante, on avait des bougies dans le pool (et sur quels symboles)
        Map<LocalDateTime, List<String>> poolPresence = buildPoolPresenceIndex(pool);

        List<MinuteGap> gaps = expected.stream()
                .filter(min -> !produced.contains(min))
                .sorted()
                .map(min -> {
                    List<String> symbols = poolPresence.getOrDefault(min, List.of());
                    return new MinuteGap(min, symbols, !symbols.isEmpty());
                })
                .toList();

        double coverage = expectedCount == 0 ? 100.0 : (100.0 * producedCount / (double) expectedCount);

        return new RolloverCoverageReport(expectedCount, producedCount, coverage, gaps);
    }

}
