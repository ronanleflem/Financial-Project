package finance.project.api.controllers;

import finance.project.api.config.StrategyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class ConfigController {

    private final StrategyConfig strategyConfig;

    @Autowired
    public ConfigController(StrategyConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

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
