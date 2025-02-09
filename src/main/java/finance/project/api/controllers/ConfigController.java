package finance.project.api.controllers;

import finance.project.api.config.StrategyConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public class ConfigController {

    private final StrategyConfig strategyConfig;

    @GetMapping("/strategies")
    public ResponseEntity<List<String>> getActiveStrategies() {
        return ResponseEntity.ok(strategyConfig.getEnabledStrategies());
    }

    @GetMapping("/filters")
    public ResponseEntity<List<String>> getActiveFilters() {
        return ResponseEntity.ok(strategyConfig.getEnabledFilters());
    }

    @PostMapping("/strategies")
    public ResponseEntity<String> updateStrategies(@RequestBody List<String> strategies) {
        strategyConfig.setEnabledStrategies(strategies);
        return ResponseEntity.ok("✅ Stratégies mises à jour : " + strategies);
    }

    @PostMapping("/filters")
    public ResponseEntity<String> updateFilters(@RequestBody List<String> filters) {
        strategyConfig.setEnabledFilters(filters);
        return ResponseEntity.ok("✅ Filtres mis à jour : " + filters);
    }
}
