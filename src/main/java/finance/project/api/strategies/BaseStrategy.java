package finance.project.api.strategies;

import finance.project.api.entities.MarketData;
import finance.project.api.model.TradeSignalDTO;
import finance.project.api.scoring.TradeScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseStrategy {

    protected final TradeScoringService tradeScoringService;
    protected static final Logger logger = LoggerFactory.getLogger(BaseStrategy.class);

    public BaseStrategy(TradeScoringService tradeScoringService) {
        this.tradeScoringService = tradeScoringService;
    }

    /**
     * Vérifie si le trade est valide selon le système de scoring.
     */
    public boolean isTradeValid(TradeSignalDTO signal, MarketData marketData) {
        return tradeScoringService.isTradeValid(signal, marketData);
    }

    /**
     * Logique spécifique de chaque stratégie (ex. breakout, mean reversion...).
     */
    protected abstract TradeSignalDTO generateTradeSignal(MarketData marketData);

    /**
     * Exécute la stratégie complète : génère un signal, valide et exécute si ok.
     */
    public void execute(MarketData marketData) {
        TradeSignalDTO signal = generateTradeSignal(marketData);

        if (signal != null && isTradeValid(signal, marketData)) {
            logger.info("🚀 [STRAT {}] Exécution du trade {}", this.getClass().getSimpleName(), signal);
            executeTrade(signal);
        } else {
            logger.info("❌ [STRAT {}] Pas de trade valide trouvé.", this.getClass().getSimpleName());
        }
    }

    /**
     * Simule l'exécution du trade (à remplacer par l'intégration avec un broker plus tard).
     */
    private void executeTrade(TradeSignalDTO signal) {
        //logger.info("🟢 Exécution trade : {} {} à {}", signal.getDirection(), signal.getAsset(), signal.getPrice());
    }
}
