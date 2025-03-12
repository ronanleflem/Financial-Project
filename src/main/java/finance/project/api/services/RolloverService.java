package finance.project.api.services;

import finance.project.api.entities.Symbology;
import finance.project.api.repositories.SymbologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolloverService {

    private final SymbologyRepository symbologyRepository;

    /**
     * Récupère le contrat dominant pour une date donnée.
     */
    public Optional<Symbology> getDominantContract(LocalDate date) {
        // Cherche tous les symbology actifs sur cette date
        List<Symbology> activeContracts = symbologyRepository.findAll().stream()
                .filter(s -> !date.isBefore(s.getStartDate()) && !date.isAfter(s.getEndDate()))
                .toList();

        if (activeContracts.isEmpty()) {
            log.warn("❌ Aucun contrat actif pour la date {}", date);
            return Optional.empty();
        }

        // Trie par score (volume, liquidité, dominance...)
        Optional<Symbology> dominant = activeContracts.stream()
                .max(Comparator.comparing(Symbology::getScore));

        dominant.ifPresent(s -> log.info("✅ Contrat dominant le {} : {} (Score: {})", date, s.getSymbol(), s.getScore()));

        return dominant;
    }

    /**
     * Renvoie la date de rollover idéale : la veille du passage à un nouveau contrat dominant.
     */
    public Optional<LocalDate> getRolloverDateForSymbol(String currentSymbol) {
        // Cherche toutes les périodes de ce symbole
        List<Symbology> periods = symbologyRepository.findBySymbol(currentSymbol);

        if (periods.isEmpty()) {
            log.warn("❌ Aucun symbology pour le symbole {}", currentSymbol);
            return Optional.empty();
        }

        // Prend la dernière période de dominance
        Symbology lastPeriod = periods.stream()
                .max(Comparator.comparing(Symbology::getEndDate))
                .orElse(null);

        if (lastPeriod == null) {
            log.warn("❌ Pas de période trouvée pour le symbole {}", currentSymbol);
            return Optional.empty();
        }

        // Rollover idéal : la veille de la fin de dominance
        LocalDate rolloverDate = lastPeriod.getEndDate().minusDays(1);

        log.info("🔄 Rollover conseillé de {} à la date {}", currentSymbol, rolloverDate);

        return Optional.of(rolloverDate);
    }

    /**
     * Récupère le prochain contrat après celui en cours.
     */
    public Optional<Symbology> getNextContractAfter(String currentSymbol) {
        // Prend la date de fin de dominance du symbole actuel
        List<Symbology> periods = symbologyRepository.findBySymbol(currentSymbol);

        if (periods.isEmpty()) {
            log.warn("❌ Aucun symbology pour {}", currentSymbol);
            return Optional.empty();
        }

        LocalDate currentEndDate = periods.stream()
                .max(Comparator.comparing(Symbology::getEndDate))
                .map(Symbology::getEndDate)
                .orElse(null);

        if (currentEndDate == null) return Optional.empty();

        // Cherche le contrat qui commence juste après
        return symbologyRepository.findAll().stream()
                .filter(s -> s.getStartDate().isAfter(currentEndDate))
                .min(Comparator.comparing(Symbology::getStartDate));
    }

}
