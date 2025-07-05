package finance.project.api.controllers;

import finance.project.api.services.StrategyDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * REST controller exposing the list of available TA4J strategies.
 */
@Controller
public class StrategyController {

    private final StrategyDiscoveryService strategyDiscoveryService;

    public StrategyController(StrategyDiscoveryService strategyDiscoveryService) {
        this.strategyDiscoveryService = strategyDiscoveryService;
    }

    /**
     * Returns the simple class names of all strategies located in
     * {@code finance.project.api.strategies.ta4j}.
     *
     * @return list of strategy names
     */
    @GetMapping("/strategies")
    public ResponseEntity<List<String>> listStrategies() {
        return ResponseEntity.ok(strategyDiscoveryService.getAvailableStrategies());
    }
}
