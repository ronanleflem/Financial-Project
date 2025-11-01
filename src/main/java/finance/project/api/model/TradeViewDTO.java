package finance.project.api.model;

import java.time.Instant;

public record TradeViewDTO(
        String broker,         // "IBKR", "XYZ", ...
        String account,        // Compte source
        String symbol,         // "EURUSD", "AAPL", "ESZ5", ...
        String description,    // Texte lisible (ex: "EUR.USD IDEALPRO (CASH)")
        String secType,        // CASH/STK/FUT/OPT/...
        String currency,       // USD/EUR/...
        String exchange,       // IDEALPRO/SMART/GLOBEX/...
        Integer conid,         // IB conid si dispo
        double position,       // qty >0 long, <0 short
        double avgCost,        // prix moyen (base currency du contract)
        Double marketPrice,    // peut être null si non enrichi
        Double marketValue,    // peut être null
        Double unrealizedPnL,  // peut être null
        Double realizedPnL,    // peut être null
        long asOfMillis        // Instant.now() à la capture
) { }