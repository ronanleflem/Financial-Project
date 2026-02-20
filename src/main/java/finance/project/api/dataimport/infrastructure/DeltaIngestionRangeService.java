package finance.project.api.dataimport.infrastructure;

import finance.project.api.config.DeltaLakeConfig;
import finance.project.api.dataimport.dto.DeltaIngestionRangeResponse;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.actions.AddFile;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public DeltaIngestionRangeService(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public List<DeltaIngestionRangeResponse> findRanges(String symbol,
                                                        String insertedType,
                                                        String timeframe,
                                                        int limit) {
        int effectiveLimit = limit <= 0 ? 200 : Math.min(limit, 5_000);
        String tablePath = stripTrailingSlash(deltaLakeConfig.getBaseUri()) + "/data";
        Configuration conf = createHadoopConfiguration();
        DeltaLog deltaLog = DeltaLog.forTable(conf, tablePath);
        if (!deltaLog.tableExists()) {
            return List.of();
        }

        List<DeltaIngestionRangeResponse> all = new ArrayList<>();
        Path tableRoot = new Path(tablePath);
        for (AddFile addFile : deltaLog.snapshot().getAllFiles()) {
            Path filePath = resolveFilePath(tableRoot, addFile.getPath());
            all.addAll(readParquet(filePath, conf));
        }

        return all.stream()
                .filter(item -> matches(item.symbol(), symbol))
                .filter(item -> matches(item.insertedType(), insertedType))
                .filter(item -> matches(item.timeframe(), timeframe))
                .sorted(Comparator.comparing(DeltaIngestionRangeResponse::insertedAt).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    private List<DeltaIngestionRangeResponse> readParquet(Path filePath, Configuration conf) {
        List<DeltaIngestionRangeResponse> results = new ArrayList<>();
        try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(filePath)
                .withConf(conf)
                .build()) {
            GenericRecord record;
            while ((record = reader.read()) != null) {
                String symbol = asString(record.get("symbol"));
                String insertedType = asString(record.get("insertedType"));
                String timeframe = asString(record.get("timeframe"));
                Long startEpochMs = asLong(record.get("startDateEpochMs"));
                Long endEpochMs = asLong(record.get("endDateEpochMs"));
                Long insertedAtEpochMs = asLong(record.get("insertedAtEpochMs"));
                if (startEpochMs == null || endEpochMs == null || insertedAtEpochMs == null) {
                    continue;
                }
                results.add(new DeltaIngestionRangeResponse(
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
        return results;
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
