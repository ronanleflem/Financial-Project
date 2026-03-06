package finance.project.api.model.run;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RunRequestInputDeserializer extends StdDeserializer<RunRequestInput> {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    public RunRequestInputDeserializer() {
        super(RunRequestInput.class);
    }

    @Override
    public RunRequestInput deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        if (codec == null) {
            return (RunRequestInput) ctxt.handleUnexpectedToken(RunRequestInput.class, parser);
        }

        JsonNode root = codec.readTree(parser);
        if (root == null || !root.isObject()) {
            return (RunRequestInput) ctxt.handleUnexpectedToken(RunRequestInput.class, parser);
        }

        ObjectMapper mapper = codec instanceof ObjectMapper objectMapper ? objectMapper : new ObjectMapper();

        String specType = textValue(firstNode(root, "specType", "spec_type"));
        String catalogVersion = textValue(firstNode(root, "catalogVersion", "catalog_version"));
        String requestId = textValue(firstNode(root, "requestId", "request_id"));
        RunType runType = treeToValue(mapper, ctxt, firstNode(root, "runType", "run_type"), RunType.class);
        if (runType == null && specType != null) {
            runType = RunType.fromValue(specType);
        }

        DataBlock data = deserializeData(mapper, ctxt, runType, root.get("data"));
        StrategyBlock strategy = deserializeStrategy(mapper, ctxt, runType, root.get("strategy"));

        BacktestSignalBlock signal = treeToValue(mapper, ctxt, root.get("signal"), BacktestSignalBlock.class);
        MarketStatsBlock stats = treeToValue(mapper, ctxt, root.get("stats"), MarketStatsBlock.class);
        SeasonalityBlock seasonality = treeToValue(mapper, ctxt, root.get("seasonality"), SeasonalityBlock.class);
        FiltersBlock filters = treeToValue(mapper, ctxt, root.get("filters"), FiltersBlock.class);
        PerformanceBlock performance = treeToValue(mapper, ctxt, root.get("performance"), PerformanceBlock.class);

        Map<String, Object> output = treeToMap(mapper, ctxt, root.get("output"));
        PersistenceSpec persistence = treeToValue(mapper, ctxt, root.get("persistence"), PersistenceSpec.class);

        return new RunRequestInput(
                specType,
                catalogVersion,
                requestId,
                runType,
                data,
                strategy,
                signal,
                stats,
                seasonality,
                filters,
                performance,
                output,
                persistence
        );
    }

    private static DataBlock deserializeData(ObjectMapper mapper, DeserializationContext ctxt, RunType runType, JsonNode node)
            throws IOException {
        if (node == null || node.isNull() || runType == null) {
            return null;
        }
        return switch (runType) {
            case BACKTEST -> mapper.treeToValue(node, BacktestDataBlock.class);
            case DCA -> mapper.treeToValue(node, DcaDataBlock.class);
            case MARKET_STATS -> mapper.treeToValue(node, MarketStatsDataBlock.class);
            case SEASONALITY -> mapper.treeToValue(node, SeasonalityDataBlock.class);
            case STRESS_TESTS -> mapper.treeToValue(node, StressTestsDataBlock.class);
        };
    }

    private static StrategyBlock deserializeStrategy(ObjectMapper mapper, DeserializationContext ctxt, RunType runType, JsonNode node)
            throws IOException {
        if (node == null || node.isNull() || runType == null) {
            return null;
        }
        return switch (runType) {
            case BACKTEST -> mapper.treeToValue(node, BacktestStrategyBlock.class);
            case DCA -> deserializeDcaStrategy(mapper, ctxt, node);
            case MARKET_STATS, SEASONALITY, STRESS_TESTS -> null;
        };
    }

    private static DcaStrategyCore deserializeDcaStrategy(ObjectMapper mapper, DeserializationContext ctxt, JsonNode node)
            throws IOException {
        DcaStrategyType type = treeToValue(mapper, ctxt, node.get("type"), DcaStrategyType.class);
        List<String> grid = node.hasNonNull("grid") ? mapper.convertValue(node.get("grid"), STRING_LIST_TYPE) : null;

        JsonNode paramsNode = node.get("params");
        DcaParams params = null;
        if (type != null && paramsNode != null && !paramsNode.isNull()) {
            params = switch (type) {
                case DCA_EQUITY -> mapper.treeToValue(paramsNode, DcaEquityParams.class);
                case DCA_ETF -> mapper.treeToValue(paramsNode, DcaEtfParams.class);
                case CRYPTO_GRID -> mapper.treeToValue(paramsNode, CryptoGridParams.class);
            };
        }
        return new DcaStrategyCore(type, grid, params);
    }

    private static <T> T treeToValue(ObjectMapper mapper, DeserializationContext ctxt, JsonNode node, Class<T> type)
            throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return mapper.treeToValue(node, type);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(node.asText(), type, e.getMessage());
        }
    }

    private static Map<String, Object> treeToMap(ObjectMapper mapper, DeserializationContext ctxt, JsonNode node)
            throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return mapper.convertValue(node, MAP_TYPE);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(node.asText(), Map.class, e.getMessage());
        }
    }

    private static String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || "null".equals(text) ? null : text;
    }

    private static JsonNode firstNode(JsonNode root, String... names) {
        if (root == null || names == null) {
            return null;
        }
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }
}
