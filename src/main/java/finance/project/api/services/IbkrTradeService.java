package finance.project.api.services;

import com.ib.client.Decimal;
import finance.project.api.model.PortfolioPositionDTO;
import finance.project.api.model.PortfolioSnapshotDTO;
import finance.project.api.model.TradeViewDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IbkrTradeService implements BrokerTradeService {

    private final IbkrFxService ib; // réutilise TA connexion + EReader

    public IbkrTradeService(IbkrFxService ib) {
        this.ib = ib;
    }

    @Override
    public String brokerId() { return "IBKR"; }

    private static double toDouble(Decimal dec) {
        if (dec == null) return 0d;
        String s = dec.toString();
        if (s == null || s.isBlank() || "NaN".equalsIgnoreCase(s)) return 0d;
        try {
            return new BigDecimal(s).doubleValue();
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
    @Override
    public List<TradeViewDTO> listOpenTrades() throws Exception {
        if (!ib.isConnected()) {
            throw new IllegalStateException("IBKR non connecté. Appelle d’abord /ibkr/connect/wait.");
        }
        long now = Instant.now().toEpochMilli();

        // 1) tentative reqPositions()
        var pos = ib.fetchOpenPositions(4000L);
        if (pos != null && !pos.isEmpty()) {
            return pos.stream()
                    .filter(p -> Math.abs(p.position()) > 1e-9)  // garde les positions non nulles
                    .map(p -> {
                        var c = p.contract();
                        return new TradeViewDTO(
                                "IBKR",
                                p.account(),
                                buildSymbol(c),
                                buildDescription(c),
                                safe(c.secType().getApiString()),
                                safe(c.currency()),
                                safe(c.exchange()),
                                c.conid() > 0 ? c.conid() : null,
                                p.position(),
                                p.avgCost(),
                                null, null, null, null,
                                now
                        );
                    })
                    .toList();
        }

        // 2) fallback enrichi via reqAccountUpdates()
        var pf = ib.fetchPortfolioSnapshot(5000L);
        return pf.stream()
                //.filter(l -> Math.abs(toDouble(l.position())) > 1e-9)
                .map(l -> {
                    var c = l.contract();
                    return new TradeViewDTO(
                            "IBKR",
                            l.account(),
                            buildSymbol(c),
                            buildDescription(c),
                            safe(c.secType().getApiString()),
                            safe(c.currency()),
                            safe(c.exchange()),
                            c.conid() > 0 ? c.conid() : null,
                            toDouble(l.position()),
                            l.averageCost(),
                            l.marketPrice(),
                            l.marketValue(),
                            l.unrealizedPNL(),
                            l.realizedPNL(),
                            now
                    );
                })
                .toList();
    }

    @Override
    public PortfolioSnapshotDTO fetchPortfolioSnapshot() throws Exception {
        if (!ib.isConnected()) {
            throw new IllegalStateException("IBKR non connecté. Appelle d’abord /ibkr/connect/wait.");
        }
        long now = Instant.now().toEpochMilli();

        var snapshot = ib.fetchPortfolioSnapshot(5000L);
        double totalMarketValue = snapshot.stream()
                .mapToDouble(IbkrFxService.IbPortfolioLine::marketValue)
                .sum();
        /*
        double availableLiquidity = snapshot.stream()
                .filter(IbkrTradeService::isCashLine)
                .mapToDouble(IbkrFxService.IbPortfolioLine::marketValue)
                .sum();*/
        double investedMarketValue = snapshot.stream()
                .filter(line -> !isCashLine(line))
                .mapToDouble(IbkrFxService.IbPortfolioLine::marketValue)
                .sum();

        List<PortfolioPositionDTO> positions = snapshot.stream()
                .map(line -> {
                    var contract = line.contract();
                    double marketValue = line.marketValue();
                    double relative = (!isCashLine(line) && Math.abs(investedMarketValue) > 1e-9)
                            ? marketValue / investedMarketValue
                            : 0d;
                    return new PortfolioPositionDTO(
                            "IBKR",
                            line.account(),
                            buildSymbol(contract),
                            buildDescription(contract),
                            safe(contract.secType().getApiString()),
                            safe(contract.currency()),
                            safe(contract.exchange()),
                            contract.conid() > 0 ? contract.conid() : null,
                            toDouble(line.position()),
                            line.marketPrice(),
                            marketValue,
                            line.averageCost(),
                            line.unrealizedPNL(),
                            line.realizedPNL(),
                            relative
                    );
                })
                .toList();

        IbkrFxService.IbAccountSnapshot acc = ib.fetchIbAccountSnapshot(3000L);

        double availableLiquidity = acc.availableFunds();

        return new PortfolioSnapshotDTO(
                "IBKR",
                totalMarketValue,
                availableLiquidity,
                positions,
                now
        );
    }


    private static String safe(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static String buildSymbol(com.ib.client.Contract c) {
        String sec = c.secType().getApiString();
        if ("CASH".equalsIgnoreCase(sec)) {
            return (safe(c.symbol()) + safe(c.currency())).replace("null", "");
        }
        // Futures ex: ES Dec25 -> tradingClass + lastTradeDateOrContractMonth
        if ("FUT".equalsIgnoreCase(sec)) {
            String m = c.lastTradeDateOrContractMonth(); // yyyyMM
            return safe(c.symbol()) + (m != null ? m : "");
        }
        // Par défaut : symbol (et tickerId si dispo)
        return safe(c.symbol());
    }

    private static String buildDescription(com.ib.client.Contract c) {
        String base = (safe(c.symbol()) + "." + safe(c.currency()) + " " + safe(c.exchange())
                + " (" + safe(c.secType().getApiString()) + ")").replace("null", "");
        return base.trim();
    }

    private static boolean isCashLine(IbkrFxService.IbPortfolioLine line) {
        var secType = line.contract().secType();
        String type = secType != null ? secType.getApiString() : null;
        return type != null && type.equalsIgnoreCase("CASH");
    }
}
