package finance.project.api.dataimport.infrastructure;

import finance.project.api.config.DeltaLakeConfig;
import finance.project.api.dataimport.dto.DeltaIngestionRangeResponse;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.Snapshot;
import io.delta.standalone.actions.AddFile;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeltaIngestionRangeService {

    private static final Logger log = LoggerFactory.getLogger(DeltaIngestionRangeService.class);

    private final DeltaLakeConfig deltaLakeConfig;
    private volatile CacheEntry cacheEntry;

    public DeltaIngestionRangeService(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public List<DeltaIngestionRangeResponse> findRanges(String symbol,
                                                        String insertedType,
                                                        String timeframe,
                                                        int limit) {
        int effectiveLimit = limit <= 0 ? 200 : Math.min(limit, 5_000);
        return getCachedOrLoadRanges(symbol, insertedType, timeframe, effectiveLimit);
    }

    private List<DeltaIngestionRangeResponse> getCachedOrLoadRanges(String symbol,
                                                                    String insertedType,
                                                                    String timeframe,
                                                                    int effectiveLimit) {
        String tablePath = stripTrailingSlash(deltaLakeConfig.getBaseUri()) + "/data";
        Configuration conf = createHadoopConfiguration();
        DeltaLog deltaLog = DeltaLog.forTable(conf, tablePath);
        if (!deltaLog.tableExists()) {
            return List.of();
        }
        Snapshot snapshot = deltaLog.update();
        long snapshotVersion = snapshot.getVersion();
        String queryKey = buildQueryKey(symbol, insertedType, timeframe, effectiveLimit);

        CacheEntry current = cacheEntry;
        if (current != null && current.snapshotVersion() == snapshotVersion && current.queryKey().equals(queryKey)) {
            return current.values();
        }

        synchronized (this) {
            CacheEntry inLock = cacheEntry;
            if (inLock != null && inLock.snapshotVersion() == snapshotVersion && inLock.queryKey().equals(queryKey)) {
                return inLock.values();
            }
            List<DeltaIngestionRangeResponse> loaded = loadRanges(snapshot, tablePath, conf, symbol, insertedType, timeframe, effectiveLimit);
            cacheEntry = new CacheEntry(snapshotVersion, queryKey, loaded);
            return loaded;
        }
    }

    private List<DeltaIngestionRangeResponse> loadRanges(Snapshot snapshot,
                                                         String tablePath,
                                                         Configuration conf,
                                                         String symbol,
                                                         String insertedType,
                                                         String timeframe,
                                                         int effectiveLimit) {
        Path tableRoot = new Path(tablePath);
        List<AddFile> files = new ArrayList<>();
        snapshot.getAllFiles().forEach(files::add);
        files.sort(Comparator.comparingLong(this::modificationTimeSafe).reversed());

        List<DeltaIngestionRangeResponse> all = new ArrayList<>(Math.min(files.size(), effectiveLimit > 0 ? effectiveLimit : files.size()));
        for (AddFile addFile : files) {
            Optional<DeltaIngestionRangeResponse> fromTags = fromAddFileTags(addFile);
            if (fromTags.isPresent()) {
                DeltaIngestionRangeResponse item = fromTags.get();
                if (matches(item.symbol(), symbol) && matches(item.insertedType(), insertedType) && matches(item.timeframe(), timeframe)) {
                    all.add(item);
                }
            } else {
                Path filePath = resolveFilePath(tableRoot, addFile.getPath());
                Optional<DeltaIngestionRangeResponse> item = readFirstParquetRow(filePath, conf);
                if (item.isPresent()) {
                    DeltaIngestionRangeResponse value = item.get();
                    if (matches(value.symbol(), symbol) && matches(value.insertedType(), insertedType) && matches(value.timeframe(), timeframe)) {
                        all.add(value);
                    }
                }
            }
            if (all.size() >= effectiveLimit) {
                break;
            }
        }

        all.sort(Comparator.comparing(DeltaIngestionRangeResponse::insertedAt).reversed());
        return all;
    }

    private Optional<DeltaIngestionRangeResponse> readFirstParquetRow(Path filePath, Configuration conf) {
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(filePath)
                .withConf(conf)
                .build()) {
            GenericRecord record = reader.read();
            if (record != null) {
                String symbol = asString(record.get("symbol"));
                String insertedType = asString(record.get("insertedType"));
                String timeframe = asString(record.get("timeframe"));
                Long startEpochMs = asLong(record.get("startDateEpochMs"));
                Long endEpochMs = asLong(record.get("endDateEpochMs"));
                Long insertedAtEpochMs = asLong(record.get("insertedAtEpochMs"));
                if (startEpochMs == null || endEpochMs == null || insertedAtEpochMs == null) {
                    return Optional.empty();
                }
                return Optional.of(new DeltaIngestionRangeResponse(
                        symbol,
                        insertedType,
                        Instant.ofEpochMilli(startEpochMs),
                        Instant.ofEpochMilli(endEpochMs),
                        timeframe,
                        Instant.ofEpochMilli(insertedAtEpochMs)
                ));
            }
        } catch (IOException e) {
            log.warn("[Delta] Failed to read ingestion range parquet {}: {}", filePath, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<DeltaIngestionRangeResponse> fromAddFileTags(AddFile addFile) {
        Map<String, String> tags = addFileTagsSafe(addFile);
        if (tags == null || tags.isEmpty()) {
            return Optional.empty();
        }
        String symbol = tags.get("symbol");
        String insertedType = tags.get("insertedType");
        String timeframe = tags.get("timeframe");
        Long startEpochMs = parseLong(tags.get("startDateEpochMs"));
        Long endEpochMs = parseLong(tags.get("endDateEpochMs"));
        Long insertedAtEpochMs = parseLong(tags.get("insertedAtEpochMs"));
        if (startEpochMs == null || endEpochMs == null || insertedAtEpochMs == null) {
            return Optional.empty();
        }
        return Optional.of(new DeltaIngestionRangeResponse(
                defaultString(symbol),
                defaultString(insertedType),
                Instant.ofEpochMilli(startEpochMs),
                Instant.ofEpochMilli(endEpochMs),
                defaultString(timeframe),
                Instant.ofEpochMilli(insertedAtEpochMs)
        ));
    }

    private static boolean matches(String value, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return value != null && value.equalsIgnoreCase(expected.trim());
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long asLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> addFileTagsSafe(AddFile addFile) {
        try {
            Object value = addFile.getClass().getMethod("getTags").invoke(addFile);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, String>) map;
            }
        } catch (Exception ignored) {
        }
        return Map.of();
    }

    private long modificationTimeSafe(AddFile addFile) {
        try {
            Object value = addFile.getClass().getMethod("getModificationTime").invoke(addFile);
            if (value instanceof Long l) {
                return l;
            }
            if (value instanceof Number n) {
                return n.longValue();
            }
        } catch (Exception ignored) {
        }
        return 0L;
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

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String buildQueryKey(String symbol, String insertedType, String timeframe, int limit) {
        return (symbol == null ? "" : symbol.trim().toLowerCase())
                + "|"
                + (insertedType == null ? "" : insertedType.trim().toLowerCase())
                + "|"
                + (timeframe == null ? "" : timeframe.trim().toLowerCase())
                + "|"
                + limit;
    }

    private record CacheEntry(long snapshotVersion, String queryKey, List<DeltaIngestionRangeResponse> values) {
    }
}
