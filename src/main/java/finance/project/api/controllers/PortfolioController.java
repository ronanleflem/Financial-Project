package finance.project.api.controllers;

import finance.project.api.model.PortfolioSnapshotDTO;
import finance.project.api.services.BrokerTradeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final Map<String, BrokerTradeService> servicesById;

    public PortfolioController(List<BrokerTradeService> services) {
        Map<String, BrokerTradeService> map = new HashMap<>();
        for (var s : services) {
            map.put(s.brokerId().toUpperCase(Locale.ROOT), s);
        }
        this.servicesById = Map.copyOf(map);
    }

    @GetMapping
    public PortfolioSnapshotDTO fetchPortfolio(@RequestParam(defaultValue = "IBKR") String broker) throws Exception {
        BrokerTradeService svc = servicesById.get(broker.toUpperCase(Locale.ROOT));
        if (svc == null) {
            throw new IllegalArgumentException("Unknown broker: " + broker + " (available: " + servicesById.keySet() + ")");
        }
        return svc.fetchPortfolioSnapshot();
    }
}
