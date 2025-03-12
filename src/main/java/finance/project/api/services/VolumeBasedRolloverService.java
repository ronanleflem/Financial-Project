package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.repositories.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolumeBasedRolloverService {

    private final CandleRepository candleRepository;

    public List<Candle> getDynamicRolloverCandlesBasedOnVolume(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int analysisPeriodDays) {

        List<Candle> resultCandles = new ArrayList<>();

        // Pointeur de la période courante
        LocalDateTime currentStart = startDateTime;

        log.info("🚀 Démarrage de l'analyse dynamique sur la période {} -> {}", startDateTime, endDateTime);

        while (currentStart.isBefore(endDateTime)) {

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

        return resultCandles;
    }
}
