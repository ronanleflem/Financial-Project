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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolumeBasedRolloverService {

    private final CandleRepository candleRepository;
    private final CandleMapper candleMapper;

    private static final LocalTime SESSION_START_CT = LocalTime.of(17, 0);

    private enum AdjustType { DIFFERENTIAL, RATIO }
    private static final AdjustType DEFAULT_ADJUST = AdjustType.DIFFERENTIAL;
    private static final int OVERLAP_MINUTES = 10;

    private static final boolean RELAX_PREV_NEXT = true;

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

    private static String fmt(CandleDTO c) {
        if (c == null) return "null";
        return String.format("sym=%s O=%.6f H=%.6f L=%.6f C=%.6f V=%s",
                c.getSymbolFuture(),
                c.getOpen()  == null ? null : c.getOpen(),
                c.getHigh()  == null ? null : c.getHigh(),
                c.getLow()   == null ? null : c.getLow(),
                c.getClose() == null ? null : c.getClose(),
                c.getVolume()== null ? null : c.getVolume());
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

    private CandleDTO buildSyntheticSecondary(LocalDateTime minute,
                                              CandleDTO prev, CandleDTO next,
                                              CandleDTO adjustedSecondary,
                                              String timeframe,
                                              SymbolDTO baseSymbol) {

        BigDecimal open  = prev.getClose();
        BigDecimal close = next.getOpen();

        BigDecimal high  = adjustedSecondary.getHigh();
        BigDecimal low   = adjustedSecondary.getLow();

        // Cohérence minimale avec O/C
        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);
        if (high.compareTo(hiBase) < 0) high = hiBase;
        if (low.compareTo(loBase) > 0)  low  = loBase;

        return CandleDTO.builder()
                .date(minute)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_SECONDARY)
                .build();
    }

    private CandleDTO buildSyntheticEmpty(LocalDateTime minute,
                                          CandleDTO prev, CandleDTO next,
                                          String timeframe,
                                          SymbolDTO baseSymbol) {
        BigDecimal prevClose = prev.getClose();
        BigDecimal nextOpen  = next.getOpen();

        BigDecimal open  = prevClose;
        BigDecimal close = nextOpen;
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


    public List<CandleDTO> backfillOneMinuteGapsWithSynthetic(
            List<CandleDTO> rolled,
            List<CandleDTO> pool,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive) {

        if (rolled == null || rolled.isEmpty()) return rolled;

        Map<LocalDateTime, CandleDTO> byMinute = new HashMap<>();
        for (CandleDTO c : rolled) {
            if (c == null || c.getDate() == null) continue;
            byMinute.put(floorToMinute(c.getDate()), c);
        }

        Map<LocalDateTime, CandleDTO> secondaryAtMinute = buildBestSecondaryByMinute(pool);
        log.info("🔧 Backfill pool size={} | secondaryAtMinute keys={}", pool.size(), secondaryAtMinute.size());

        // NEW: index par symbole/minute sur tout le pool (ou au moins front + secondaire autour de m)
        Map<String, Map<LocalDateTime, CandleDTO>> bySymMin = mapBySymbolMinute(pool);

        Set<LocalDateTime> expected = expectedTradableMinutes(
                floorToMinute(startInclusive), floorToMinute(endExclusive));

        String timeframe = Optional.ofNullable(rolled.get(0).getTimeframe()).orElse("1min");
        SymbolDTO baseSymbol = rolled.get(0).getSymbol();

        List<CandleDTO> patched = new ArrayList<>(rolled);
        int addedSecondary = 0, addedEmpty = 0;
        int poolHasButMapMiss = 0;
        for (LocalDateTime m : expected) {
            if (byMinute.containsKey(m)) continue;

            LocalDateTime prevMin = m.minusMinutes(1);
            LocalDateTime nextMin = m.plusMinutes(1);
            CandleDTO prev = byMinute.get(prevMin);
            CandleDTO next = byMinute.get(nextMin);
            boolean havePrev = (prev != null);
            boolean haveNext = (next != null);

            if (!havePrev || !haveNext) {
                if (RELAX_PREV_NEXT) {
                    for (int k = 2; k <= 5 && (!havePrev || !haveNext); k++) {
                        if (!havePrev) { prev = byMinute.get(m.minusMinutes(k)); havePrev = (prev != null); }
                        if (!haveNext) { next = byMinute.get(m.plusMinutes(k)); haveNext = (next != null); }
                    }
                }
            }

            // Candidate secondaire à la minute m (meilleur volume)
            CandleDTO sec = secondaryAtMinute.get(m);
            if (sec == null) {
                sec = pickBestOutrightAtMinute(bySymMin, m);
            }

            // DEBUG: état initial à la minute
            if (log.isDebugEnabled()) {
                log.debug("🔎 MISSING {} | prev:{} | next:{} | secCandidate:{}",
                        m, havePrev ? prev.getSymbolFuture() : "∅",
                        haveNext ? next.getSymbolFuture() : "∅",
                        (sec != null ? sec.getSymbolFuture() : "∅"));
            }

            // Si aucune secondary candidate trouvée mais on avait des bougies brutes → expliquer
            if (sec == null) {
                Map<LocalDateTime, List<String>> dbgPresence = buildPoolPresenceIndex(pool);
                List<String> dbgSyms = dbgPresence.getOrDefault(m, List.of());
                if (!dbgSyms.isEmpty()) {
                    poolHasButMapMiss++; // FIX: on incrémente réellement ce compteur
                    // On avait du brut, mais tous exclus (spreads? filtrage?) → log détaillé
                    log.info("🔎 {}: brut présent mais aucune SECONDARY exploitable | rawSymbols={} (spreads exclus, horodatage exact requis)",
                            m, dbgSyms);
                }
            }

            CandleDTO synth;
            if (sec != null) {
                // Déterminer le "front" pour l'ajustement
                String frontSym = norm(havePrev ? prev.getSymbolFuture() : haveNext ? next.getSymbolFuture() : sec.getSymbolFuture());
                String secSym   = norm(sec.getSymbolFuture());

                // Coeffs d'ajustement locaux
                AdjustCoeffs adj = (frontSym != null && frontSym.equals(secSym))
                        ? new AdjustCoeffs(0.0, 1.0, DEFAULT_ADJUST)
                        : computeLocalAdjust(frontSym, secSym, m, bySymMin, OVERLAP_MINUTES, DEFAULT_ADJUST);

                CandleDTO secAdj = applyAdjust(sec, adj);

                // Log opportunité SECONDARY (avant fabrication)
                log.info("🧪 SECONDARY_OPPORTUNITY {} | front={} havePrev={} haveNext={} | sec={} | adjType={} A={} R={}",
                        m, frontSym, havePrev, haveNext, secSym, adj.type, adj.A, adj.R);

                // Garde-fou: si OHLC null → impossible de bâtir une vraie SECONDARY
                if (secAdj.getOpen()==null || secAdj.getHigh()==null || secAdj.getLow()==null || secAdj.getClose()==null) {
                    log.warn("🚫 SECONDARY_ABORT {} | Raison=OHLC incomplet après ajustement | secAdj={}", m, fmt(secAdj));
                    synth = (havePrev && haveNext)
                            ? buildSyntheticEmpty(m, prev, next, timeframe, baseSymbol)
                            : CandleDTO.builder()
                            .date(m)
                            .open((havePrev?prev.getClose():haveNext?next.getOpen():BigDecimal.ZERO))
                            .close((havePrev?prev.getClose():haveNext?next.getOpen():BigDecimal.ZERO))
                            .high((havePrev?prev.getClose():haveNext?next.getOpen():BigDecimal.ZERO))
                            .low((havePrev?prev.getClose():haveNext?next.getOpen():BigDecimal.ZERO))
                            .volume(BigDecimal.ZERO)
                            .timeframe(timeframe)
                            .symbol(baseSymbol)
                            .symbolFuture(SYNTHETIC_EMPTY)
                            .build();
                    addedEmpty++;
                } else {
                    if (havePrev && haveNext) {
                        synth = buildSyntheticSecondary(m, prev, next, secAdj, timeframe, baseSymbol);
                        log.info("✅ SECONDARY_USED {} | mode=BOTH_SIDES | sec={} | prev={} | next={}",
                                m, secSym, prev.getSymbolFuture(), next.getSymbolFuture());
                    } else if (havePrev) {
                        synth = buildSyntheticSecondaryOneSided(m, prev, true, secAdj, timeframe, baseSymbol);
                        log.info("✅ SECONDARY_USED {} | mode=ONE_SIDED(prev) | sec={} | prev={}",
                                m, secSym, prev.getSymbolFuture());
                    } else if (haveNext) {
                        synth = buildSyntheticSecondaryOneSided(m, next, false, secAdj, timeframe, baseSymbol);
                        log.info("✅ SECONDARY_USED {} | mode=ONE_SIDED(next) | sec={} | next={}",
                                m, secSym, next.getSymbolFuture());
                    } else {
                        // Dernier recours : O=C sur secAdj.close/open
                        BigDecimal oc = secAdj.getClose() != null ? secAdj.getClose() : secAdj.getOpen();
                        if (oc == null) {
                            log.warn("🚫 SECONDARY_ABORT {} | Raison=pas de voisin et pas de prix secAdj | secAdj={}", m, fmt(secAdj));
                            synth = buildSyntheticEmpty(m,
                                    CandleDTO.builder().close(BigDecimal.ZERO).build(),
                                    CandleDTO.builder().open(BigDecimal.ZERO).build(),
                                    timeframe, baseSymbol);
                            addedEmpty++;
                        } else {
                            BigDecimal high = secAdj.getHigh() != null ? secAdj.getHigh() : oc;
                            BigDecimal low  = secAdj.getLow()  != null ? secAdj.getLow()  : oc;
                            synth = CandleDTO.builder()
                                    .date(m).open(oc).close(oc)
                                    .high(high.max(oc)).low(low.min(oc))
                                    .volume(BigDecimal.ZERO)
                                    .timeframe(timeframe)
                                    .symbol(baseSymbol)
                                    .symbolFuture(SYNTHETIC_SECONDARY)
                                    .build();
                            log.info("✅ SECONDARY_USED {} | mode=PURE_SEC (O=C) | sec={}", m, secSym);
                        }
                        if (synth.getSymbolFuture().equals(SYNTHETIC_SECONDARY)) addedSecondary++; else addedEmpty++;
                    }

                    if (synth.getSymbolFuture().equals(SYNTHETIC_SECONDARY)) addedSecondary++;
                }
            } else {
                // Aucune secondary → EMPTY
                if (havePrev || haveNext) {
                    CandleDTO side = havePrev ? prev : next;
                    BigDecimal oc = havePrev ? side.getClose() : side.getOpen();
                    synth = CandleDTO.builder()
                            .date(m).open(oc).close(oc)
                            .high(oc).low(oc)
                            .volume(BigDecimal.ZERO)
                            .timeframe(timeframe)
                            .symbol(baseSymbol)
                            .symbolFuture(SYNTHETIC_EMPTY)
                            .build();
                    log.info("ℹ️ EMPTY_USED {} | raison=NO_SECONDARY_AVAILABLE | sideOnly={} | sideSym={}",
                            m, havePrev ? "prev" : "next", side.getSymbolFuture());
                } else {
                    synth = buildSyntheticEmpty(m,
                            CandleDTO.builder().close(BigDecimal.ZERO).build(),
                            CandleDTO.builder().open(BigDecimal.ZERO).build(),
                            timeframe, baseSymbol);
                    log.info("ℹ️ EMPTY_USED {} | raison=NO_NEIGHBORS_NO_SECONDARY", m);
                }
                addedEmpty++;
            }

            patched.add(synth);
            byMinute.put(m, synth);
        }
        log.info("📈 Minutes où pool avait des bougies mais sec=null: {}", poolHasButMapMiss);
        patched.sort(Comparator.comparing(CandleDTO::getDate));
        log.info("🩹 Backfill M1: {} SYNTHETIC_SECONDARY, {} SYNTHETIC_EMPTY ajoutées", addedSecondary, addedEmpty);
        return patched;
    }

    public record TimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {}

    public static class ContractMissing {
        public final String symbol;
        public final List<TimeRange> missingRanges;
        public final int expectedMinutes;
        public final int presentMinutes;
        public final double coveragePct;

        public ContractMissing(String symbol, List<TimeRange> missingRanges,
                               int expectedMinutes, int presentMinutes) {
            this.symbol = symbol;
            this.missingRanges = missingRanges;
            this.expectedMinutes = expectedMinutes;
            this.presentMinutes = presentMinutes;
            this.coveragePct = expectedMinutes == 0 ? 100.0 : (100.0 * presentMinutes / expectedMinutes);
        }
    }

    public static class MultiContractsMissingReport {
        public final List<String> symbols;                 // les N contrats étudiés (ordre = ranking)
        public final List<ContractMissing> perContract;    // détail par contrat
        public final List<TimeRange> missingAll;           // manquantes sur TOUS les N (intersection)
        public final List<TimeRange> missingAny;           // manquantes sur AU MOINS 1 des N (union)
        public final int expectedMinutes;                  // minutes tradables totales (mêmes pour tous)

        public MultiContractsMissingReport(List<String> symbols,
                                           List<ContractMissing> perContract,
                                           List<TimeRange> missingAll,
                                           List<TimeRange> missingAny,
                                           int expectedMinutes) {
            this.symbols = symbols;
            this.perContract = perContract;
            this.missingAll = missingAll;
            this.missingAny = missingAny;
            this.expectedMinutes = expectedMinutes;
        }
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

    private List<TimeRange> compressConsecutiveMinutes(List<LocalDateTime> minutesSorted) {
        List<TimeRange> out = new ArrayList<>();
        if (minutesSorted.isEmpty()) return out;

        LocalDateTime runStart = minutesSorted.get(0);
        LocalDateTime prev = runStart;

        for (int i = 1; i < minutesSorted.size(); i++) {
            LocalDateTime cur = minutesSorted.get(i);
            if (!cur.equals(prev.plusMinutes(1))) {
                out.add(new TimeRange(runStart, prev.plusMinutes(1))); // [start, prev+1min)
                runStart = cur;
            }
            prev = cur;
        }
        out.add(new TimeRange(runStart, prev.plusMinutes(1)));
        return out;
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

    private Map<String, Set<LocalDateTime>> minutesBySymbol(Set<String> keepSymbols, List<CandleDTO> candles) {
        Map<String, Set<LocalDateTime>> map = new HashMap<>();
        for (CandleDTO c : candles) {
            if (c == null || c.getDate() == null) continue;
            String sym = c.getSymbolFuture();
            if (sym == null || sym.isBlank()) continue;
            if (sym.contains("-")) continue; // ignore spreads type "6EM5-6EH5"
            if (!keepSymbols.contains(sym)) continue;

            map.computeIfAbsent(sym, k -> new HashSet<>()).add(floorToMinute(c.getDate()));
        }
        return map;
    }

// ===============================================
// Fonction principale demandée
// ===============================================
    /**
     * Détecte les plages M1 manquantes pour les deux contrats présents dans `candles`.
     * - Choisit les 2 contrats les plus représentés (en nombre de bougies) en ignorant les spreads "X-Y".
     * - Respecte le calendrier CME (break, week-end, horaires).
     * - Retourne les ranges manquants par contrat + l'intersection (manquants sur les deux).
     */
    public MultiContractsMissingReport findMissingRangesForNContracts(
            List<CandleDTO> candles,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            int topN,
            boolean rankByVolume
    ) {
        if (candles == null || candles.isEmpty()) throw new IllegalArgumentException("Liste de candles vide");
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive))
            throw new IllegalArgumentException("Fenêtre temporelle invalide");
        if (topN < 1) throw new IllegalArgumentException("topN doit être ≥ 1");

        // 1) Agrégats par contrat (hors spreads)
        Map<String, Long> countBySym = new HashMap<>();
        Map<String, Double> volBySym = new HashMap<>();
        for (CandleDTO c : candles) {
            if (c == null || c.getDate() == null) continue;
            String sym = c.getSymbolFuture();
            if (sym == null || sym.isBlank() || sym.contains("-")) continue;
            countBySym.merge(sym, 1L, Long::sum);
            double v = (c.getVolume() != null) ? c.getVolume().doubleValue() : 0.0;
            volBySym.merge(sym, v, Double::sum);
        }
        if (countBySym.isEmpty()) throw new IllegalStateException("Aucun contrat outright détecté.");

        // 2) Sélection topN
        Comparator<String> cmp = rankByVolume
                ? Comparator.<String>comparingDouble(sym -> volBySym.getOrDefault(sym, 0.0)).reversed()
                : Comparator.<String>comparingLong(sym -> countBySym.getOrDefault(sym, 0L)).reversed();

        List<String> symbols = countBySym.keySet().stream().sorted(cmp).limit(topN).toList();

        // 3) Minutes attendues
        Set<LocalDateTime> expected = expectedTradableMinutes(
                floorToMinute(startInclusive), floorToMinute(endExclusive));
        int expectedCount = expected.size();

        // 4) Minutes présentes par symbole
        Map<String, Set<LocalDateTime>> presentBySym = new HashMap<>();
        for (String s : symbols) presentBySym.put(s, new HashSet<>());
        for (CandleDTO c : candles) {
            if (c == null || c.getDate() == null) continue;
            String sym = c.getSymbolFuture();
            if (sym == null || sym.isBlank() || sym.contains("-")) continue;
            if (!presentBySym.containsKey(sym)) continue; // on ne garde que les topN
            LocalDateTime m = floorToMinute(c.getDate());
            if (m.isBefore(startInclusive) || !m.isBefore(endExclusive)) continue;
            // On ne compte que les minutes tradables
            if (!expected.contains(m)) continue;
            presentBySym.get(sym).add(m);
        }

        // 5) Missing par contrat + stats
        List<ContractMissing> per = new ArrayList<>();
        List<Set<LocalDateTime>> missingSets = new ArrayList<>();
        for (String s : symbols) {
            Set<LocalDateTime> present = presentBySym.getOrDefault(s, Set.of());
            List<LocalDateTime> missingList = expected.stream()
                    .filter(min -> !present.contains(min))
                    .sorted()
                    .toList();
            missingSets.add(new HashSet<>(missingList));
            List<TimeRange> ranges = compressConsecutiveMinutes(missingList);
            int presentCount = expectedCount == 0 ? 0 : expectedCount - missingList.size();
            per.add(new ContractMissing(s, ranges, expectedCount, presentCount));
        }

        // 6) Intersection (minutes manquantes sur TOUS les N)
        Set<LocalDateTime> inter = new HashSet<>(expected);
        for (Set<LocalDateTime> s : missingSets) inter.retainAll(s);
        List<TimeRange> missingAll = compressConsecutiveMinutes(inter.stream().sorted().toList());

        // 7) Union (minutes manquantes sur AU MOINS 1)
        Set<LocalDateTime> uni = new HashSet<>();
        for (Set<LocalDateTime> s : missingSets) uni.addAll(s);
        List<TimeRange> missingAny = compressConsecutiveMinutes(uni.stream().sorted().toList());

        // Logs utiles
        log.info("🔎 Top{} contrats ({}): {}", symbols.size(), rankByVolume ? "par volume" : "par count", symbols);
        for (ContractMissing cm : per) {
            log.info("   {} → couverture={}%, expected={}, present={}, ranges manquants={}",
                    cm.symbol, String.format(Locale.US,"%.2f", cm.coveragePct),
                    cm.expectedMinutes, cm.presentMinutes, cm.missingRanges.size());
            if (!cm.missingRanges.isEmpty()) {
                TimeRange r = cm.missingRanges.get(0);
                log.debug("     ex range: {} → {}", r.startInclusive(), r.endExclusive());
            }
        }
        if (!missingAll.isEmpty()) {
            TimeRange r = missingAll.get(0);
            log.warn("❗ Manquantes sur TOUS ({}) : {} ranges (ex: {} → {})",
                    symbols, missingAll.size(), r.startInclusive(), r.endExclusive());
        }

        return new MultiContractsMissingReport(symbols, per, missingAll, missingAny, expectedCount);
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

    // Récupère les candles d’un symbol [start, end)
    private List<CandleDTO> getWindowForSymbol(CandleIndex idx, String symbol, LocalDateTime start, LocalDateTime end) {
        NavigableMap<LocalDateTime, List<CandleDTO>> map = idx.bySymbolThenDate.get(symbol);
        if (map == null || start == null || end == null || !start.isBefore(end)) return Collections.emptyList();
        NavigableMap<LocalDateTime, List<CandleDTO>> sub = map.subMap(start, true, end, false);
        if (sub.isEmpty()) return Collections.emptyList();
        List<CandleDTO> out = new ArrayList<>();
        for (List<CandleDTO> bucket : sub.values()) out.addAll(bucket);
        return out;
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

        // --- Nettoyage minimal (ignore null, dates null, spreads) ---
        List<CandleDTO> src = inputCandles.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.getDate() != null)
                .filter(c -> c.getSymbolFuture() != null && !c.getSymbolFuture().isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
        if (src.isEmpty()) {
            log.warn("⚠️ Après filtrage, plus aucune bougie exploitable.");
            return Collections.emptyList();
        }

        // --- Index principaux ---
        CandleIndex idx = buildIndex(src);

        // bySymbolMinute[symbol][minute] = candle
        Map<String, Map<LocalDateTime, CandleDTO>> bySymbolMinute = new HashMap<>();
        Map<String, Double> globalVol = new HashMap<>();
        for (CandleDTO c : src) {
            String sym = c.getSymbolFuture();
            if (sym.contains("-")) continue; // ignore spreads
            LocalDateTime m = floorToMinute(c.getDate());
            bySymbolMinute.computeIfAbsent(sym, k -> new HashMap<>()).put(m, c);
            double v = (c.getVolume() != null) ? c.getVolume().doubleValue() : 0.0;
            globalVol.merge(sym, v, Double::sum);
        }

        // Ranking GLOBAL par volume (desc)
        List<String> globalRanking = globalVol.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        // --- Minutes attendues GLOBAL (UTC) puis sous-ensembles par session ---
        NavigableSet<LocalDateTime> expectedAll = new TreeSet<>(
                expectedTradableMinutes(
                        startDateTime.withSecond(0).withNano(0),
                        endDateTime.withSecond(0).withNano(0)
                )
        );

        // Helper : classement par volume sur fenêtre [a,b)
        java.util.function.BiFunction<LocalDateTime, LocalDateTime, List<String>> rankSymbolsByVolume =
                (a, b) -> {
                    List<CandleDTO> window = getWindow(idx, a, b);
                    if (window.isEmpty()) return List.of();
                    return window.stream()
                            .filter(c -> !c.getSymbolFuture().contains("-"))
                            .collect(Collectors.groupingBy(
                                    CandleDTO::getSymbolFuture,
                                    Collectors.summingDouble(c -> c.getVolume() != null ? c.getVolume().doubleValue() : 0.0)
                            ))
                            .entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .map(Map.Entry::getKey)
                            .toList();
                };

        List<CandleDTO> out = new ArrayList<>();
        LocalDateTime sessionStart = sessionStartOnOrBefore(startDateTime);

        while (sessionStart.isBefore(endDateTime)) {
            LocalDateTime sessionNext = nextSessionStart(sessionStart);

            // Fenêtre effective bornée par la plage globale
            LocalDateTime windowStart = sessionStart.isBefore(startDateTime) ? startDateTime : sessionStart;
            LocalDateTime windowEnd   = sessionNext.isAfter(endDateTime)     ? endDateTime   : sessionNext;
            if (!windowStart.isBefore(windowEnd)) break;

            // Minute “pivot” tradable ? (samedi/ven 17h, etc.)
            if (!isTradableSessionStart(sessionStart)) {
                sessionStart = sessionNext;
                continue;
            }

            // Classement SESSION (fenêtre analyse N jours *Chicago*)
            LocalDateTime analysisEnd   = sessionStart;
            LocalDateTime analysisStart = toUtc(toChi(sessionStart).minusDays(analysisPeriodDays));
            List<String> sessionRanking = rankSymbolsByVolume.apply(analysisStart, analysisEnd);

            List<String> orderList = new ArrayList<>();
            if (!sessionRanking.isEmpty()) {
                orderList.addAll(sessionRanking);
            } else {
                // NEW: fallback global si aucune donnée dans la fenêtre d’analyse (1ʳᵉ session typiquement)
                orderList.addAll(globalRanking);
                if (orderList.isEmpty()) {
                    sessionStart = sessionNext;
                    continue; // rien du tout à proposer
                }
                log.warn("🧭 Session {} → {} : sessionRanking vide, fallback sur globalRanking ({}…)",
                        sessionStart, sessionNext, orderList.get(0));
            }
            String dominant = orderList.get(0);

            // Ordre de fallback : dominant → (reste de la liste) – déjà sans doublons
            LinkedHashSet<String> order = new LinkedHashSet<>(orderList);

            // Minutes attendues pour CETTE session (issues du set GLOBAL)
            NavigableSet<LocalDateTime> expectedSession = expectedAll.subSet(
                    windowStart.withSecond(0).withNano(0), true,
                    windowEnd.withSecond(0).withNano(0), false
            );

            int usedDominant = 0, usedFallback = 0, skipped = 0;

            for (LocalDateTime m : expectedSession) {
                CandleDTO chosen = null;

                // 1) dominant
                Map<LocalDateTime, CandleDTO> mdom = bySymbolMinute.get(dominant);
                if (mdom != null) {
                    chosen = mdom.get(m);
                }

                // 2) fallback : autres symbols (session → global)
                if (chosen == null) {
                    for (String sym : order) {
                        if (sym.equals(dominant)) continue;
                        Map<LocalDateTime, CandleDTO> mm = bySymbolMinute.get(sym);
                        if (mm == null) continue;
                        CandleDTO alt = mm.get(m);
                        if (alt != null) {
                            chosen = alt;
                            usedFallback++;
                            break;
                        }
                    }
                } else {
                    usedDominant++;
                }

                if (chosen != null) {
                    out.add(chosen);
                } else {
                    skipped++; // laissé à la synthèse
                }
            }

            log.info("📦 Session {} → {} | dominant={} | minutes: dominant={}, fallback={}, skippedForSynth={}",
                    sessionStart, sessionNext, dominant, usedDominant, usedFallback, skipped);

            sessionStart = sessionNext;
        }

        // Ordonner + log coverage pré-synthèse
        out.sort(Comparator.comparing(CandleDTO::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        if (!out.isEmpty()) {
            LocalDateTime s = out.get(0).getDate();
            LocalDateTime e = out.get(out.size()-1).getDate().plusMinutes(1);
            Set<LocalDateTime> expected = expectedTradableMinutes(floorToMinute(s), floorToMinute(e));
            int uniq = (int) out.stream().map(c -> c.getDate().withSecond(0).withNano(0)).distinct().count();
            double cov = expected.isEmpty() ? 100.0 : 100.0 * uniq / expected.size();
            log.info("✅ Coverage après session+fallback GLOBAL (avant synthèse) {} → {} : uniques={} / attendus={} ({}%)",
                    s, e, uniq, expected.size(), String.format(Locale.US, "%.2f", cov));
        }


        RolloverCoverageReport report = auditRolloverCoverage(
                out,           // série produite
                inputCandles,     // pool brut
                startDateTime,
                endDateTime
        );

        log.info("📊 ROLLOVER COVERAGE: produced={} / expected={} ({}%)",
                report.getProducedMinutes(), report.getExpectedMinutes(),
                String.format(Locale.US, "%.2f", report.getCoveragePct()));

        report.getMissing().stream().limit(5).forEach(g -> {
            log.info("⛔ Missing {} | in raw: {} {}",
                    g.getMinute(),
                    g.isPresentInPool() ? "YES" : "NO",
                    g.isPresentInPool() ? g.getAvailableSymbols() : "");
        });

        return out;
    }

    private LocalDateTime plusDaysChicago(LocalDateTime utc, int days) {
        return toUtc(toChi(utc).plusDays(days)); // avance d'1 jour *local Chicago*
    }
    private boolean isSaturdayChicago(LocalDateTime utc) {
        return toChi(utc).getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    public List<CandleDTO> getDynamicRolloverCandlesPerMinuteWithFallback(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays) {

        List<Candle> resultCandles = new ArrayList<>();
        LocalDateTime currentMinute = startDateTime;

        log.info("🚀 Démarrage de l'analyse minute par minute avec fallback sur la période {} -> {}", startDateTime, endDateTime);

        while (!currentMinute.isAfter(endDateTime)) {

            DayOfWeek day = currentMinute.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                log.info("⏭️ Weekend ignoré : {}", currentMinute);
                currentMinute = currentMinute.plusMinutes(1);
                continue;
            }

            // Fenêtre d'analyse pour déterminer les contrats dominants
            LocalDateTime analysisStart = currentMinute.minusDays(analysisPeriodDays);
            LocalDateTime analysisEnd = currentMinute;

            log.info("🔎 Analyse volume sur la fenêtre {} -> {}", analysisStart, analysisEnd);

            List<Candle> analysisCandles = candleRepository.findByDateBetween(analysisStart, analysisEnd);

            if (analysisCandles.isEmpty()) {
                log.warn("❌ Aucune candle trouvée pour l'analyse volume de {} -> {}", analysisStart, analysisEnd);
                currentMinute = currentMinute.plusMinutes(1);
                continue;
            }

            // Classement des symboles par volume décroissant
            List<Map.Entry<String, Double>> sortedVolumes = analysisCandles.stream()
                    .collect(Collectors.groupingBy(
                            Candle::getSymbolFuture,
                            Collectors.summingDouble(c -> c.getVolume() != null ? c.getVolume().doubleValue() : 0.0)
                    ))
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .toList();

            if (sortedVolumes.isEmpty()) {
                log.warn("❌ Aucun symbole dominant sur la fenêtre {} -> {}", analysisStart, analysisEnd);
                currentMinute = currentMinute.plusMinutes(1);
                continue;
            }

            // Recherche de la candle sur cette minute, par ordre de dominance
            Candle candleForThisMinute = null;

            for (Map.Entry<String, Double> entry : sortedVolumes) {
                String candidateSymbol = entry.getKey();
                Double candidateVolume = entry.getValue();

                log.debug("🔎 Tentative récupération candle à {} pour {} (volume cumulé : {})", currentMinute, candidateSymbol, candidateVolume);

                Optional<Candle> candleOpt = candleRepository.findBySymbolFutureAndDate(
                        candidateSymbol, currentMinute
                );

                if (candleOpt.isPresent()) {
                    candleForThisMinute = candleOpt.get();
                    log.info("✅ Candle trouvée pour {} à {} : {}", candidateSymbol, currentMinute, candleForThisMinute);
                    break; // Stop dès qu'on en trouve une
                }
            }

            if (candleForThisMinute != null) {
                resultCandles.add(candleForThisMinute);
            } else {
                log.warn("⚠️ Aucune candle trouvée pour la minute {}", currentMinute);
            }

            currentMinute = currentMinute.plusMinutes(1);
        }

        // Tri final (normalement inutile, mais on assure)
        resultCandles.sort(Comparator.comparing(Candle::getDate));

        log.info("🎉 Flux final généré avec {} candles sur {} -> {}", resultCandles.size(), startDateTime, endDateTime);

        return resultCandles.stream().map(candleMapper::toDto).toList();
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

    private Map<LocalDateTime, List<String>> buildPoolPresenceIndex(List<CandleDTO> pool) {
        Map<LocalDateTime, Set<String>> tmp = new HashMap<>();
        for (CandleDTO c : pool) {
            if (c == null || c.getDate() == null) continue;
            String s = norm(c.getSymbolFuture());
            if (s == null || s.isBlank() || s.contains("-")) continue;
            LocalDateTime m = floorToMinute(c.getDate());
            tmp.computeIfAbsent(m, k -> new LinkedHashSet<>()).add(s);
        }
        Map<LocalDateTime, List<String>> out = new HashMap<>();
        tmp.forEach((k,v)-> out.put(k, new ArrayList<>(v)));
        return out;
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

    private CandleDTO buildSyntheticSecondaryOneSided(
            LocalDateTime minute,
            CandleDTO side,                // prev OU next (un seul)
            boolean isPrev,                // true si side=prev
            CandleDTO adjustedSecondary,
            String timeframe,
            SymbolDTO baseSymbol
    ) {
        // Si on n'a qu'un côté, on cale O/C sur la seule info dispo
        BigDecimal oc = isPrev ? side.getClose() : side.getOpen();
        BigDecimal open  = oc;
        BigDecimal close = oc;

        BigDecimal high = adjustedSecondary.getHigh();
        BigDecimal low  = adjustedSecondary.getLow();

        // Assurer cohérence min/max
        BigDecimal hiBase = open.max(close);
        BigDecimal loBase = open.min(close);
        if (high.compareTo(hiBase) < 0) high = hiBase;
        if (low.compareTo(loBase) > 0)  low  = loBase;

        return CandleDTO.builder()
                .date(minute)
                .open(open)
                .close(close)
                .high(high)
                .low(low)
                .volume(BigDecimal.ZERO)
                .timeframe(timeframe)
                .symbol(baseSymbol)
                .symbolFuture(SYNTHETIC_SECONDARY)
                .build();
    }

}
