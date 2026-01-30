package finance.project.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import finance.project.api.controllers.NotFoundException;
import finance.project.api.entities.StressTestResult;
import finance.project.api.repositories.StressTestResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class StressTestResultService {
    private static final String MODE_MONTE_CARLO = "monte_carlo";
    private static final String MODE_SCENARIOS = "scenarios";

    private final StressTestResultRepository stressTestResultRepository;
    private final ObjectMapper objectMapper;

    public StressTestResultService(StressTestResultRepository stressTestResultRepository, ObjectMapper objectMapper) {
        this.stressTestResultRepository = stressTestResultRepository;
        this.objectMapper = objectMapper;
    }

    public List<StressTestResult> getByRunId(String runId) {
        return stressTestResultRepository.findByRunId(runId);
    }

    public StressTestResult getById(Long id) {
        return stressTestResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("StressTestResult not found: " + id));
    }

    public ObjectNode buildSummary(String runId) {
        List<StressTestResult> results = stressTestResultRepository.findByRunId(runId);
        if (results.isEmpty()) {
            return null;
        }

        StressTestResult monteCarlo = results.stream()
                .filter(result -> MODE_MONTE_CARLO.equalsIgnoreCase(result.getMode()))
                .findFirst()
                .orElse(null);

        StressTestResult scenarios = results.stream()
                .filter(result -> MODE_SCENARIOS.equalsIgnoreCase(result.getMode()))
                .findFirst()
                .orElse(null);

        StressTestResult base = monteCarlo != null ? monteCarlo : results.get(0);

        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("strategyId", base.getStrategyId());
        meta.put("runId", base.getRunId());
        meta.put("assetClass", base.getAssetClass());
        meta.put("symbol", base.getSymbol());
        meta.put("timeframe", base.getTimeframe());
        response.set("meta", meta);

        response.set("monteCarlo", monteCarlo != null ? mapMonteCarlo(monteCarlo) : NullNode.getInstance());
        response.set("scenarios", scenarios != null ? mapScenarios(scenarios) : NullNode.getInstance());

        return response;
    }

    private ObjectNode mapMonteCarlo(StressTestResult result) {
        JsonNode root = readPayload(result);
        ObjectNode out = objectMapper.createObjectNode();

        JsonNode parameters = root.path("parameters");
        ObjectNode parametersOut = objectMapper.createObjectNode();
        if (parameters.has("n_simulations")) {
            parametersOut.set("nSimulations", parameters.get("n_simulations"));
        }
        if (parameters.has("initial_capital")) {
            parametersOut.set("initialCapital", parameters.get("initial_capital"));
        }
        if (parametersOut.size() > 0) {
            out.set("parameters", parametersOut);
        }

        JsonNode metrics = root.path("metrics");
        if (!metrics.isMissingNode() && !metrics.isNull()) {
            out.set("metricsByDistribution", metrics);
        }

        JsonNode distributions = root.path("distributions");
        ObjectNode curves = objectMapper.createObjectNode();
        JsonNode equityCurves = distributions.path("equity_curves");
        if (!equityCurves.isMissingNode() && !equityCurves.isNull()) {
            curves.set("equitySample", equityCurves);
        }
        JsonNode level1 = distributions.path("level1");
        if (!level1.isMissingNode() && !level1.isNull()) {
            curves.set("level1", level1);
        }
        if (curves.size() > 0) {
            out.set("curves", curves);
        }

        return out;
    }

    private ObjectNode mapScenarios(StressTestResult result) {
        JsonNode root = readPayload(result);
        ObjectNode out = objectMapper.createObjectNode();

        JsonNode metrics = root.path("metrics").path("scenarios");
        JsonNode distributions = root.path("distributions").path("scenarios");
        JsonNode parameters = root.path("parameters").path("scenarios");

        int metricsSize = metrics.isArray() ? metrics.size() : 0;
        int distributionsSize = distributions.isArray() ? distributions.size() : 0;
        int parametersSize = parameters.isArray() ? parameters.size() : 0;
        int size = Math.max(metricsSize, Math.max(distributionsSize, parametersSize));

        ArrayNode items = objectMapper.createArrayNode();
        for (int i = 0; i < size; i++) {
            ObjectNode item = objectMapper.createObjectNode();
            if (parameters.isArray() && parameters.size() > i && !parameters.get(i).isNull()) {
                item.set("settings", parameters.get(i));
            }
            if (metrics.isArray() && metrics.size() > i && !metrics.get(i).isNull()) {
                item.set("metrics", metrics.get(i));
            }
            if (distributions.isArray() && distributions.size() > i && !distributions.get(i).isNull()) {
                item.set("curve", distributions.get(i));
            }
            items.add(item);
        }

        out.set("items", items);
        return out;
    }

    private JsonNode readPayload(StressTestResult result) {
        try {
            return objectMapper.readTree(Objects.toString(result.getPayloadJson(), "{}"));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid stress test payload JSON for id=" + result.getId(), ex);
        }
    }
}
