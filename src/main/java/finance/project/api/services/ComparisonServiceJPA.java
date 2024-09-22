package finance.project.api.services;

import finance.project.api.controllers.NotFoundException;
import finance.project.api.entities.Candle;
import finance.project.api.entities.Comparison;
import finance.project.api.entities.Symbol;
import finance.project.api.model.PerformanceComparisonDTO;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.ComparisonRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Service
@Primary
@RequiredArgsConstructor
public class ComparisonServiceJPA implements ComparisonService {

    private final CandleRepository candleRepository;
    private final SymbolRepository symbolRepository;
    private final ComparisonRepository comparisonRepository;

    public PerformanceComparisonDTO comparePerformance(String symbol1, String symbol2, LocalDate startDate, LocalDate endDate) {
        Optional<Symbol> optionalSymbol1 = symbolRepository.findBySymbol(symbol1);
        Optional<Symbol> optionalSymbol2 = symbolRepository.findBySymbol(symbol2);

        if (optionalSymbol1.isEmpty() || optionalSymbol2.isEmpty()) {
            throw new NotFoundException("Un ou les deux symboles ne sont pas trouvés.");
        }

        List<Candle> symbol1Candles = candleRepository.findBySymbolAndDateBetween(optionalSymbol1.get(), startDate, endDate);
        List<Candle> symbol2Candles = candleRepository.findBySymbolAndDateBetween(optionalSymbol2.get(), startDate, endDate);

        BigDecimal performance1 = calculatePerformance(symbol1Candles);
        BigDecimal performance2 = calculatePerformance(symbol2Candles);

        Symbol symbolEntity1 = symbol1Candles.get(0).getSymbol();
        Symbol symbolEntity2 = symbol2Candles.get(0).getSymbol();

        Comparison comparison = new Comparison(
                UUID.randomUUID(),
                symbolEntity1,
                symbolEntity2,
                performance1,
                performance2,
                startDate,
                endDate
        );
        comparisonRepository.save(comparison);

        return new PerformanceComparisonDTO(symbol1, performance1, symbol2, performance2);
    }

    /**
     * Calcule la performance d'un actif en pourcentage sur la période représentée par une liste de bougies.
     *
     * @param candles la liste des bougies représentant les prix d'un actif sur une période
     * @return la performance de l'actif en pourcentage (positive ou négative)
     */
    private BigDecimal calculatePerformance(List<Candle> candles) {
        // Vérifie si la liste de bougies est vide
        // Si elle est vide, il n'y a pas de données de prix disponibles pour la période
        if (candles.isEmpty()) {
            // Retourne 0 pour indiquer qu'aucune performance ne peut être calculée
            return BigDecimal.ZERO;
        }

        // Récupère le prix de clôture de la première bougie de la période
        // Ce prix représente le prix de départ de l'actif au début de la période
        BigDecimal startPrice = candles.get(0).getClose();

        // Récupère le prix de clôture de la dernière bougie de la période
        // Ce prix représente le prix final de l'actif à la fin de la période
        BigDecimal endPrice = candles.get(candles.size() - 1).getClose();

        // Calcule la différence entre le prix final et le prix de départ
        // Cette différence représente l'augmentation ou la diminution absolue de la valeur de l'actif
        // Ensuite, divise cette différence par le prix de départ pour obtenir le changement relatif (en pourcentage)
        // Utilise RoundingMode.HALF_UP pour arrondir correctement le résultat
        // Enfin, multiplie le résultat par 100 pour obtenir un pourcentage
        return (endPrice.subtract(startPrice)) // Différence entre le prix final et le prix de départ
                .divide(startPrice,10, RoundingMode.HALF_UP) // Division par le prix de départ pour obtenir un ratio
                .multiply(BigDecimal.valueOf(100)); // Multiplication par 100 pour obtenir un pourcentage
    }
}