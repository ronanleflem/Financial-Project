package finance.project.api.services;

import finance.project.api.model.TradeViewDTO;

import java.util.List;

public interface BrokerTradeService {
    /** Identifiant court du broker (ex: "IBKR") */
    String brokerId();

    /** Liste des positions/trades en cours (timeout interne raisonnable) */
    List<TradeViewDTO> listOpenTrades() throws Exception;
}