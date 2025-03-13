package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.mappers.CandleMapper;
import finance.project.api.model.CandleDTO;
import finance.project.api.repositories.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.time.DayOfWeek;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolumeBasedRolloverService {

    private final CandleRepository candleRepository;
    private final CandleMapper candleMapper;

    public List<CandleDTO> getDynamicRolloverCandlesBasedOnVolumeOld(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays) {

        List<Candle> resultCandles = new ArrayList<>();

        // Pointeur de la période courante
        LocalDateTime currentStart = startDateTime;

        log.info("🚀 Démarrage de l'analyse dynamique sur la période {} -> {}", startDateTime, endDateTime);

        while (currentStart.isBefore(endDateTime)) {

            DayOfWeek day = currentStart.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                log.info("⏭️ Skip weekend : {}", currentStart);
                currentStart = currentStart.plusDays(1);
                continue;
            }

            // Période d'analyse
            LocalDateTime analysisStart = currentStart.minusDays(analysisPeriodDays);
            LocalDateTime analysisEnd = currentStart;

            log.info("🔎 Fenêtre d'analyse de {} à {}", analysisStart, analysisEnd);

            // 1. On récupère les candles sur la période d'analyse
            List<Candle> analysisCandles = candleRepository.findByDateBetween(analysisStart, analysisEnd);

            if (analysisCandles.isEmpty()) {
                log.warn("❌ Aucune candle trouvée pour l'analyse de la fenêtre {} -> {}", analysisStart, analysisEnd);
                // On skip et avance de 1 jour
                currentStart = currentStart.plusDays(1);
                continue;
            }

            // 2. On cumule les volumes par contrat
            Map<String, Double> volumeBySymbol = analysisCandles.stream()
                    .collect(Collectors.groupingBy(
                            Candle::getSymbolFuture,
                            Collectors.summingDouble(candle -> candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0)
                    ));

            // 3. On trouve le contrat dominant
            Optional<Map.Entry<String, Double>> dominantEntry = volumeBySymbol.entrySet().stream()
                    .max(Map.Entry.comparingByValue());

            if (dominantEntry.isEmpty()) {
                log.warn("❌ Impossible de déterminer le contrat dominant sur la fenêtre {} -> {}", analysisStart, analysisEnd);
                currentStart = currentStart.plusDays(1);
                continue;
            }

            String dominantSymbol = dominantEntry.get().getKey();
            Double dominantVolume = dominantEntry.get().getValue();

            log.info("✅ Contrat dominant : {} (volume cumulé : {}) sur la fenêtre {} -> {}", dominantSymbol, dominantVolume, analysisStart, analysisEnd);

            // 4. Définir la période de récupération des candles (par exemple 1 jour)
            LocalDateTime nextCheckPoint = currentStart.plusDays(1);
            if (nextCheckPoint.isAfter(endDateTime)) {
                nextCheckPoint = endDateTime;
            }

            // 5. On récupère les candles pour le contrat dominant sur cette sous-période
            List<Candle> candlesForPeriod = candleRepository.findBySymbolFutureAndDateBetween(
                    dominantSymbol, currentStart, nextCheckPoint
            );

            log.info("📈 Récupéré {} candles de {} sur la période {} -> {}", candlesForPeriod.size(), dominantSymbol, currentStart, nextCheckPoint);

            resultCandles.addAll(candlesForPeriod);

            // 6. On avance d'un jour
            currentStart = nextCheckPoint;
        }

        // 7. Trier toutes les candles pour assurer une timeline fluide
        resultCandles.sort(Comparator.comparing(Candle::getDate));

        log.info("🎉 Flux final généré : {} candles sur {} -> {}", resultCandles.size(), startDateTime, endDateTime);

        return resultCandles.stream().map(candleMapper::toDto).toList();
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

}
