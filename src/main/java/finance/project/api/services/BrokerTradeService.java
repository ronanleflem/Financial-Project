package finance.project.api.services;

import finance.project.api.model.PortfolioSnapshotDTO;
import finance.project.api.model.TradeViewDTO;

import java.util.List;

public interface BrokerTradeService {
    /** Identifiant court du broker (ex: "IBKR") */
    String brokerId();

    /** Liste des positions/trades en cours (timeout interne raisonnable) */
    List<TradeViewDTO> listOpenTrades() throws Exception;

    /** Récupère un snapshot agrégé du portefeuille du broker. */
    PortfolioSnapshotDTO fetchPortfolioSnapshot() throws Exception;
}
