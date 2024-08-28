package finance.project.api.bootstrap;

import finance.project.api.entities.Candle;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * BootstrapData est un composant Spring qui charge des données initiales dans la base de données
 * de l'application au démarrage. Elle implémente l'interface {@link CommandLineRunner}, ce qui signifie que
 * la méthode {@link #run(String...)} sera exécutée automatiquement lorsque l'application démarre.
 *
 * Cette classe est principalement responsable de l'initialisation de la base de données avec des données par
 * défaut pour les Candles et les Symbols si la base de données est vide.
 */
@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    /**
     * Répertoire pour gérer les objets de type {@link Candle}.
     */
    private final CandleRepository candleRepository;

    /**
     * Répertoire pour gérer les objets de type {@link Symbol}.
     */
    private final SymbolRepository symbolRepository;

    /**
     * Cette méthode est exécutée automatiquement au démarrage de l'application.
     * Elle appelle les méthodes pour charger les données par défaut des Candles et des Symbols dans la base de données.
     *
     * @param args arguments de la ligne de commande passés à l'application
     * @throws Exception en cas d'erreur lors du chargement des données
     */
    @Override
    public void run(String... args) throws Exception {
        loadCandleData();
        loadSymbolData();
    }

    /**
     * Charge les données par défaut des Candles dans la base de données si aucun enregistrement de Candle n'est présent.
     * Cette méthode crée et sauvegarde un ensemble prédéfini d'objets Candle.
     */
    private void loadCandleData() {
        if (candleRepository.count() == 0) {
            Candle chart1 = createCandle("AAPL", "100.00", "110.00", "115.00", "95.00", "1000000");
            Candle chart2 = createCandle("EURUSD", "110.00", "120.00", "125.00", "105.00", "1100000");
            Candle chart3 = createCandle("GOLD", "110.00", "120.00", "125.00", "105.00", "1100000");

            candleRepository.saveAll(Arrays.asList(chart1, chart2, chart3));
        }
    }

    /**
     * Crée un nouvel objet Candle avec les attributs fournis.
     *
     * @param symbol le symbole de négociation associé à la bougie
     * @param open le prix d'ouverture de la bougie
     * @param close le prix de fermeture de la bougie
     * @param high le prix le plus élevé de la bougie
     * @param low le prix le plus bas de la bougie
     * @param volume le volume des transactions pour la bougie
     * @return un nouvel objet {@link Candle}
     */
    private Candle createCandle(String symbol, String open, String close, String high, String low, String volume) {
        return Candle.builder()
                .symbol(symbol)
                .date(LocalDate.now())
                .open(new BigDecimal(open))
                .close(new BigDecimal(close))
                .high(new BigDecimal(high))
                .low(new BigDecimal(low))
                .volume(new BigDecimal(volume))
                .build();
    }

    /**
     * Charge les données par défaut pour les objets Symbol dans la base de données si aucun enregistrement Symbol n'est présent.
     * Cette méthode crée et enregistre un ensemble prédéfini d'objets Symbol.
     */
    private void loadSymbolData() {
        if (symbolRepository.count() == 0) {
            Symbol symbol1 = createSymbol("AAPL", "Apple Inc.", "NASDAQ");
            Symbol symbol2 = createSymbol("EURUSD", "Euro to US Dollar", "Forex");
            Symbol symbol3 = createSymbol("GOLD", "Gold Futures", "Commodities");

            symbolRepository.saveAll(Arrays.asList(symbol1, symbol2, symbol3));
        }
    }

    /**
     * Crée un nouvel objet Symbol avec les attributs fournis.
     *
     * @param symbol le symbole de négociation de l'actif
     * @param name le nom complet de la société ou de l'actif associé au symbole
     * @param market le marché dans lequel le symbole est négocié
     * @return un nouvel objet {@link Symbol}
     */
    private Symbol createSymbol(String symbol, String name, String market) {
        return Symbol.builder()
                .symbol(symbol)
                .name(name)
                .market(market)
                .build();
    }



}
