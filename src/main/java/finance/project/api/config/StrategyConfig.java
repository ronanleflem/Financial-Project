package finance.project.api.config;


import lombok.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class StrategyConfig {

    @Value("${trading.strategies.enabled:}") // Gestion des valeurs vides
    private String enabledStrategies;

    @Value("${trading.filters.enabled:}")
    private String enabledFilters;

    private final Environment environment;

    public StrategyConfig(Environment environment) {
        this.environment = environment;
    }

    // 🔹 Stratégies activées dynamiquement
    public List<String> getEnabledStrategies() {
        return enabledStrategies.isEmpty() ? List.of() : Arrays.asList(enabledStrategies.split(","));
    }

    public void setEnabledStrategies(List<String> strategies) {
        this.enabledStrategies = String.join(",", strategies);
    }

    // 🔹 Filtres activés dynamiquement
    public List<String> getEnabledFilters() {
        return enabledFilters.isEmpty() ? List.of() : Arrays.asList(enabledFilters.split(","));
    }

    public void setEnabledFilters(List<String> filters) {
        this.enabledFilters = String.join(",", filters);
    }

    // 🔹 Charge dynamiquement la pondération d'un filtre
    public int getFilterWeight(String filterName) {
        String propertyKey = "trading.filters.weights." + filterName;
        return environment.getProperty(propertyKey, Integer.class, 1); // Valeur par défaut = 1
    }

    // 🔹 Mise à jour dynamique des pondérations des filtres
    public void setFilterWeights(Map<String, Integer> filterWeights) {
        filterWeights.forEach((filter, weight) ->
                System.setProperty("trading.filters.weights." + filter, String.valueOf(weight))
        );
    }
}

