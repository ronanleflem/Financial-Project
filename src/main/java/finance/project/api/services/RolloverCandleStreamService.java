package finance.project.api.services;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbology;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolloverCandleStreamService {

    private final SymbologyRepository symbologyRepository;
    private final CandleRepository candleRepository;

    /**
     * Récupère une timeline complète de candles selon la logique de rollover basée sur la symbology
     */
    public List<Candle> getUnifiedCandles(LocalDateTime startDateTime, LocalDateTime endDateTime) {

        List<Candle> resultCandles = new ArrayList<>();

        // 1. On récupère toutes les symbologies pertinentes sur la période
        List<Symbology> symbologyPeriods = symbologyRepository.findAll().stream()
                .filter(s -> !s.getSymbol().contains("-")) // option : ignorer spreads si nécessaire
                .filter(s -> !s.getEndDate().atTime(23,59,59).isBefore(startDateTime) &&
                        !s.getStartDate().atStartOfDay().isAfter(endDateTime))
                .sorted(Comparator.comparing(Symbology::getStartDate))
                .toList();

        if (symbologyPeriods.isEmpty()) {
            log.warn("❌ Aucune symbology trouvée sur la période demandée");
            return resultCandles;
        }

        log.info("🔄 {} périodes symbology trouvées pour la plage demandée", symbologyPeriods.size());

        // 2. Pour chaque période symbology, on récupère les candles du contrat dominant
        for (Symbology sym : symbologyPeriods) {

            // Définir les limites temporelles de la période à renvoyer
            LocalDateTime periodStart = sym.getStartDate().atStartOfDay();
            LocalDateTime periodEnd = sym.getEndDate().atTime(23, 59, 59);

            // Si les bornes de la symbology sortent de la période demandée, on ajuste
            if (periodStart.isBefore(startDateTime)) periodStart = startDateTime;
            if (periodEnd.isAfter(endDateTime)) periodEnd = endDateTime;

            log.info("📌 Traitement du contrat {} du {} au {}", sym.getSymbol(), periodStart, periodEnd);

            // 3. Récupérer les candles pour le contrat sur cette période
            List<Candle> candlesForPeriod = candleRepository.findBySymbolFutureAndDateBetween(
                    sym.getSymbol(), periodStart, periodEnd);

            log.info("✅ {} candles récupérées pour {}", candlesForPeriod.size(), sym.getSymbol());

            resultCandles.addAll(candlesForPeriod);
        }

        // 4. Trier toutes les candles par date pour avoir une timeline fluide
        resultCandles.sort(Comparator.comparing(Candle::getDate));

        log.info("🎉 Flux complet généré : {} candles", resultCandles.size());

        return resultCandles;
    }
}
