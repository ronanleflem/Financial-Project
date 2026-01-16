package finance.project.api.services;

import finance.project.api.config.DeltaLakeConfig;
import finance.project.api.entities.Symbol;
import finance.project.api.entities.TradeCompleted;
import finance.project.api.model.CandleDTO;
import finance.project.api.model.SymbolDTO;
import finance.project.api.utils.DurationUtils;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.actions.AddFile;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DeltaLakeCandleReader {

    private static final Logger log = LoggerFactory.getLogger(DeltaLakeCandleReader.class);

    private final DeltaLakeConfig deltaLakeConfig;

    public DeltaLakeCandleReader(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public List<CandleDTO> getCandlesForTrade(TradeCompleted trade, String timeframe, int beforeCandles, int afterCandles) {
        return getCandlesForTrade(trade, null, timeframe, beforeCandles, afterCandles);
    }

    public List<CandleDTO> getCandlesForTrade(TradeCompleted trade,
                                              Symbol symbol,
                                              String timeframe,
                                              int beforeCandles,
                                              int afterCandles) {
        if (trade == null) {
            return List.of();
        }
        Duration tfDuration = DurationUtils.parseTimeframe(timeframe);
        LocalDateTime start = trade.getEntryTimestamp().minus(tfDuration.multipliedBy(beforeCandles));
        LocalDateTime end = trade.getExitTimestamp().plus(tfDuration.multipliedBy(afterCandles));
        String tablePath = buildDeltaPath(trade, symbol);
        return loadCandles(tablePath, trade.getSymbol(), timeframe, start, end);
    }

    private List<CandleDTO> loadCandles(String tablePath, String symbol, String timeframe, LocalDateTime start, LocalDateTime end) {
        if (tablePath == null || tablePath.isBlank()) {
            return List.of();
        }
        Configuration conf = createHadoopConfiguration();
        DeltaLog deltaLog = DeltaLog.forTable(conf, stripTrailingSlash(tablePath));
        if (!deltaLog.tableExists()) {
            log.warn("[Delta] Table not found at {}", tablePath);
            return List.of();
        }

        List<CandleDTO> candles = new ArrayList<>();
        Path tableRoot = new Path(stripTrailingSlash(tablePath));
        for (AddFile addFile : deltaLog.snapshot().getAllFiles()) {
            Path filePath = resolveFilePath(tableRoot, addFile.getPath());
            candles.addAll(readParquet(filePath, conf, symbol, timeframe, start, end));
        }
        return candles;
    }

    private List<CandleDTO> readParquet(Path filePath,
                                        Configuration conf,
                                        String symbol,
                                        String timeframe,
                                        LocalDateTime start,
                                        LocalDateTime end) {
        List<CandleDTO> results = new ArrayList<>();
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(filePath)
                .withConf(conf)
                .build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                String recordTimeframe = Optional.ofNullable(record.get("timeframe"))
                        .map(Object::toString)
                        .orElse("");
                if (!recordTimeframe.equalsIgnoreCase(timeframe)) {
                    continue;
                }
                Long timestamp = (Long) record.get("timestamp");
                if (timestamp == null) {
                    continue;
                }
                LocalDateTime date = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime();
                if (date.isBefore(start) || date.isAfter(end)) {
                    continue;
                }

                results.add(CandleDTO.builder()
                        .timeframe(recordTimeframe)
                        .date(date)
                        .open(asBigDecimal(record.get("open")))
                        .close(asBigDecimal(record.get("close")))
                        .high(asBigDecimal(record.get("high")))
                        .low(asBigDecimal(record.get("low")))
                        .volume(asBigDecimal(record.get("volume")))
                        .symbol(SymbolDTO.builder().symbol(symbol).build())
                        .build());
            }
        } catch (IOException e) {
            log.warn("[Delta] Failed to read parquet {}: {}", filePath, e.getMessage());
        }
        return results;
    }

    private String buildDeltaPath(TradeCompleted trade, Symbol symbol) {
        String market = resolveMarket(trade.getAssetClass());
        boolean isCrypto = isCryptoAsset(trade.getAssetClass());
        boolean isForex = market.equals("FX");
        boolean isEtf = isEtfAsset(trade.getAssetClass());
        String broker = defaultIfBlank(trade.getBroker(), "UNKNOWN");
        String marketType = defaultIfBlank(trade.getMarketType(), "SPOT");
        String exchangeFallback = isCrypto && !"UNKNOWN".equalsIgnoreCase(broker) ? broker : "UNKNOWN";
        String exchange = defaultIfBlank(trade.getExchange(), exchangeFallback);
        if (isEtf && symbol != null && !defaultIfBlank(symbol.getExchange(), "").isBlank()) {
            exchange = symbol.getExchange();
        }
        String currencyFallback = isCrypto ? "USDT" : "USD";
        String currency = defaultIfBlank(trade.getCurrency(), currencyFallback);
        String symbolCode = defaultIfBlank(trade.getSymbol(), "UNKNOWN");
        return isCrypto || isForex ? "%s/%s/%s/%s/%s/%s".formatted(
                deltaLakeConfig.getBaseUri(),
                market,
                broker,
                marketType,
                currency,
                symbolCode
        ) : "%s/%s/%s/%s/%s/%s/%s".formatted(
                deltaLakeConfig.getBaseUri(),
                market,
                broker,
                marketType,
                exchange,
                currency,
                symbolCode
        );
    }

    private String resolveMarket(String assetClass) {
        String normalized = Optional.ofNullable(assetClass).orElse("").toUpperCase(Locale.ROOT);
        if (normalized.contains("EQUITY")) {
            return "STOCK";
        }
        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    private boolean isCryptoAsset(String assetClass) {
        String normalized = Optional.ofNullable(assetClass).orElse("").toUpperCase(Locale.ROOT);
        return normalized.contains("CRYPTO");
    }

    private boolean isEtfAsset(String assetClass) {
        String normalized = Optional.ofNullable(assetClass).orElse("").toUpperCase(Locale.ROOT);
        return normalized.equals("ETF");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return BigDecimal.valueOf(doubleValue);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }

    private Path resolveFilePath(Path tableRoot, String addFilePath) {
        if (addFilePath == null) {
            return tableRoot;
        }
        if (addFilePath.contains("://")) {
            return new Path(addFilePath);
        }
        return new Path(tableRoot, addFilePath);
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private Configuration createHadoopConfiguration() {
        Configuration configuration = new Configuration();
        configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3a.endpoint", "http://localhost:9000");
        configuration.setBoolean("fs.s3a.path.style.access", true);
        configuration.setBoolean("fs.s3a.connection.ssl.enabled", false);
        configuration.set("fs.s3a.access.key", "minioadmin");
        configuration.set("fs.s3a.secret.key", "minioadmin");
        configuration.set("fs.s3a.fast.upload", "true");
        configuration.set("fs.s3a.fast.upload.buffer", "array");
        configuration.set("fs.s3a.block.size", "8m");
        configuration.set("fs.s3a.multipart.size", "8m");
        configuration.set("fs.s3a.multipart.threshold", "8m");
        configuration.set("hadoop.tmp.dir", "C:/hadoop-tmp");
        return configuration;
    }
}
