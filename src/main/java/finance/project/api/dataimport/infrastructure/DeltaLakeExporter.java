package finance.project.api.dataimport.infrastructure;

import finance.project.api.config.DeltaLakeConfig;
import finance.project.api.dataimport.DataImportJob;
import finance.project.api.entities.Candle;
import io.delta.standalone.DeltaLog;
import io.delta.standalone.Operation;
import io.delta.standalone.OptimisticTransaction;
import io.delta.standalone.actions.AddFile;
import io.delta.standalone.actions.Action;
import io.delta.standalone.actions.Metadata;
import io.delta.storage.LocalLogStore;
import io.delta.standalone.types.DoubleType;
import io.delta.standalone.types.LongType;
import io.delta.standalone.types.StringType;
import io.delta.standalone.types.StructField;
import io.delta.standalone.types.StructType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.avro.AvroParquetWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeltaLakeExporter {

    private static final Logger log = LoggerFactory.getLogger(DeltaLakeExporter.class);

    private static final StructType CANDLE_SCHEMA = new StructType(new StructField[]{
            new StructField("timestamp", new LongType(), false),
            new StructField("open", new DoubleType(), true),
            new StructField("high", new DoubleType(), true),
            new StructField("low", new DoubleType(), true),
            new StructField("close", new DoubleType(), true),
            new StructField("volume", new DoubleType(), true),
            new StructField("broker", new StringType(), true),
            new StructField("sourceType", new StringType(), true),
            new StructField("timeframe", new StringType(), true),
            new StructField("symbol", new StringType(), true),
            new StructField("jobId", new StringType(), true)
    });
    private static final StructType RANGE_AUDIT_SCHEMA = new StructType(new StructField[]{
            new StructField("symbol", new StringType(), false),
            new StructField("insertedType", new StringType(), false),
            new StructField("startDateEpochMs", new LongType(), false),
            new StructField("endDateEpochMs", new LongType(), false),
            new StructField("timeframe", new StringType(), false),
            new StructField("insertedAtEpochMs", new LongType(), false),
            new StructField("jobId", new StringType(), true)
    });

    private static final Schema AVRO_SCHEMA = SchemaBuilder.record("candle")
            .namespace("finance.project.delta")
            .fields()
            .name("timestamp").type().longType().noDefault()
            .name("open").type().doubleType().noDefault()
            .name("high").type().doubleType().noDefault()
            .name("low").type().doubleType().noDefault()
            .name("close").type().doubleType().noDefault()
            .name("volume").type().unionOf().nullType().and().doubleType().endUnion().nullDefault()
            .name("broker").type().stringType().noDefault()
            .name("sourceType").type().stringType().noDefault()
            .name("timeframe").type().stringType().noDefault()
            .name("symbol").type().stringType().noDefault()
            .name("jobId").type().stringType().noDefault()
            .endRecord();
    private static final Schema RANGE_AUDIT_AVRO_SCHEMA = SchemaBuilder.record("ingestion_range_audit")
            .namespace("finance.project.delta")
            .fields()
            .name("symbol").type().stringType().noDefault()
            .name("insertedType").type().stringType().noDefault()
            .name("startDateEpochMs").type().longType().noDefault()
            .name("endDateEpochMs").type().longType().noDefault()
            .name("timeframe").type().stringType().noDefault()
            .name("insertedAtEpochMs").type().longType().noDefault()
            .name("jobId").type().stringType().noDefault()
            .endRecord();

    private final DeltaLakeConfig deltaLakeConfig;

    public DeltaLakeExporter(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public void exportCandlesToDelta(DataImportJob job, List<Candle> candles) {
        exportCandlesToDelta(job, candles, null, Collections.emptyMap());
    }

    public void exportCandlesToDelta(DataImportJob job, List<Candle> candles, String tablePath, Map<String, String> metadata) {
        if (job == null) {
            log.warn("[Delta] DataImportJob is null, skipping export");
            return;
        }
        if (candles == null || candles.isEmpty()) {
            log.debug("[Delta] No candles to export for job {}", job.getId());
            return;
        }

        String assetCategory = resolveAssetCategory(job);
        String effectiveTablePath = (tablePath == null || tablePath.isBlank())
                ? deltaLakeConfig.resolveTablePath(assetCategory, job.getBroker(), job.getVenue(), job.getSymbol())
                : stripTrailingSlash(tablePath);
        String conflictPolicy = Optional.ofNullable(job.getConflictPolicy())
                .map(policy -> policy.toUpperCase(Locale.ROOT))
                .orElse("MERGE");

        Configuration conf = createHadoopConfiguration();
        try {
            DeltaLog deltaLog = DeltaLog.forTable(conf, effectiveTablePath);

            if ("SKIP".equals(conflictPolicy) && deltaLog.tableExists()) {
                log.info("[Delta] Conflict policy=SKIP and table already exists at {}. Skipping export.", effectiveTablePath);
                return;
            }

            OptimisticTransaction txn = deltaLog.startTransaction();

            if (!deltaLog.tableExists()) {
                Metadata metadataAction = Metadata.builder()
                        .schema(CANDLE_SCHEMA)
                        .name(job.getSymbol())
                        .description("Candles for " + job.getSymbol())
                        .build();
                txn.updateMetadata(metadataAction);
            }

            List<Action> actions = new ArrayList<>();
            if ("OVERWRITE".equals(conflictPolicy) && deltaLog.tableExists()) {
                deltaLog.snapshot().getAllFiles().stream()
                        .map(AddFile::remove)
                        .forEach(actions::add);
            }

            Path tableRoot = new Path(effectiveTablePath);
            Path dataDir = new Path(tableRoot, "data");
            Path dataFile = new Path(dataDir, "part-" + UUID.randomUUID() + ".parquet");

            writeParquet(conf, dataFile, job, candles);

            FileSystem fs = dataFile.getFileSystem(conf);
            FileStatus status = fs.getFileStatus(dataFile);
            long fileSize = status.getLen();
            String relativePath = relativize(tableRoot, dataFile);

            AddFile addFile = new AddFile(
                    relativePath,
                    Collections.emptyMap(),
                    fileSize,
                    System.currentTimeMillis(),
                    true,
                    null,
                    Collections.emptyMap()
            );
            actions.add(addFile);

            Map<String, String> parameters = new HashMap<>();
            parameters.put("mode", "\"" + conflictPolicy + "\"");
            parameters.put("assetCategory", "\"" + assetCategory + "\"");
            if (metadata != null) {
                metadata.forEach((k, v) -> parameters.put(k, "\"" + defaultString(v) + "\""));
            }
            Operation operation = new Operation(Operation.Name.WRITE, parameters, Collections.emptyMap());

            txn.commit(actions, operation, job.getId());
            log.info("[Delta] Exported {} candles for job {} to {}", candles.size(), job.getId(), effectiveTablePath);
            appendIngestionRangeAudit(conf, job, candles, resolveInsertedType(job, assetCategory));
        } catch (Exception e) {
            log.error("[Delta] Failed to export candles for job {}: {}", job.getId(), e.getMessage(), e);
        }
    }

    private void appendIngestionRangeAudit(Configuration conf,
                                           DataImportJob job,
                                           List<Candle> candles,
                                           String insertedType) {
        try {
            LocalDateTime minDate = candles.stream()
                    .map(Candle::getDate)
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime maxDate = candles.stream()
                    .map(Candle::getDate)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            if (minDate == null || maxDate == null) {
                return;
            }

            String auditTablePath = stripTrailingSlash(deltaLakeConfig.getBaseUri()) + "/data";
            DeltaLog deltaLog = DeltaLog.forTable(conf, auditTablePath);
            OptimisticTransaction txn = deltaLog.startTransaction();

            if (!deltaLog.tableExists()) {
                Metadata metadataAction = Metadata.builder()
                        .schema(RANGE_AUDIT_SCHEMA)
                        .name("INGESTION_RANGE_AUDIT")
                        .description("Ingestion ranges for Delta inserts")
                        .build();
                txn.updateMetadata(metadataAction);
            }

            Path tableRoot = new Path(auditTablePath);
            Path dataDir = new Path(tableRoot, "data");
            Path dataFile = new Path(dataDir, "part-" + UUID.randomUUID() + ".parquet");
            writeRangeAuditParquet(conf, dataFile, job, insertedType, minDate, maxDate);

            FileSystem fs = dataFile.getFileSystem(conf);
            FileStatus status = fs.getFileStatus(dataFile);
            String relativePath = relativize(tableRoot, dataFile);

            AddFile addFile = new AddFile(
                    relativePath,
                    Collections.emptyMap(),
                    status.getLen(),
                    System.currentTimeMillis(),
                    true,
                    null,
                    Collections.emptyMap()
            );

            Operation operation = new Operation(
                    Operation.Name.WRITE,
                    Map.of("mode", "\"APPEND\"", "source", "\"INGESTION_RANGE_AUDIT\""),
                    Collections.emptyMap()
            );
            txn.commit(List.of(addFile), operation, job.getId());
            log.info("[Delta] Appended ingestion range audit for job {} at {}", job.getId(), auditTablePath);
        } catch (Exception ex) {
            log.warn("[Delta] Failed to append ingestion range audit for job {}: {}", job.getId(), ex.getMessage());
        }
    }

    private void writeParquet(Configuration conf, Path dataFile, DataImportJob job, List<Candle> candles) throws IOException {
        FileSystem fs = dataFile.getFileSystem(conf);
        if (!fs.exists(dataFile.getParent())) {
            fs.mkdirs(dataFile.getParent());
        }

        HadoopOutputFile outputFile = HadoopOutputFile.fromPath(dataFile, conf);
        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                .withSchema(AVRO_SCHEMA)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {
            for (Candle candle : candles) {
                if (candle == null || candle.getDate() == null) {
                    continue;
                }
                GenericRecord record = new GenericData.Record(AVRO_SCHEMA);
                record.put("timestamp", candle.getDate().toInstant(ZoneOffset.UTC).toEpochMilli());
                record.put("open", asDouble(candle.getOpen()));
                record.put("high", asDouble(candle.getHigh()));
                record.put("low", asDouble(candle.getLow()));
                record.put("close", asDouble(candle.getClose()));
                record.put("volume", candle.getVolume() != null ? asDouble(candle.getVolume()) : null);
                record.put("broker", defaultString(job.getBroker()));
                record.put("sourceType", defaultString(job.getSourceType()));
                record.put("timeframe", defaultString(job.getTimeframe()));
                record.put("symbol", defaultString(job.getSymbol()));
                record.put("jobId", defaultString(job.getId()));
                writer.write(record);
            }
        }
    }

    private void writeRangeAuditParquet(Configuration conf,
                                        Path dataFile,
                                        DataImportJob job,
                                        String insertedType,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate) throws IOException {
        FileSystem fs = dataFile.getFileSystem(conf);
        if (!fs.exists(dataFile.getParent())) {
            fs.mkdirs(dataFile.getParent());
        }

        HadoopOutputFile outputFile = HadoopOutputFile.fromPath(dataFile, conf);
        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                .withSchema(RANGE_AUDIT_AVRO_SCHEMA)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {
            GenericRecord record = new GenericData.Record(RANGE_AUDIT_AVRO_SCHEMA);
            record.put("symbol", defaultString(job.getSymbol()));
            record.put("insertedType", normalizeInsertedType(insertedType));
            record.put("startDateEpochMs", startDate.toInstant(ZoneOffset.UTC).toEpochMilli());
            record.put("endDateEpochMs", endDate.toInstant(ZoneOffset.UTC).toEpochMilli());
            record.put("timeframe", defaultString(job.getTimeframe()));
            record.put("insertedAtEpochMs", System.currentTimeMillis());
            record.put("jobId", defaultString(job.getId()));
            writer.write(record);
        }
    }

    private double asDouble(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String relativize(Path tableRoot, Path filePath) {
        String table = stripTrailingSlash(tableRoot.toUri().getPath());
        String file = filePath.toUri().getPath();
        if (file.startsWith(table)) {
            String relative = file.substring(table.length());
            if (relative.startsWith("/")) {
                return relative.substring(1);
            }
            return relative;
        }
        return filePath.getName();
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private Configuration createHadoopConfiguration() {
        Configuration configuration = new Configuration();
        // Impl S3A pour tous les chemins s3a://
        configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

        // MinIO est sur localhost:9000
        configuration.set("fs.s3a.endpoint", "http://localhost:9000");
        configuration.setBoolean("fs.s3a.path.style.access", true);
        configuration.setBoolean("fs.s3a.connection.ssl.enabled", false);

        // Credentials MinIO (DEV ONLY, à sortir plus tard dans la conf/env)
        configuration.set("fs.s3a.access.key", "minioadmin");
        configuration.set("fs.s3a.secret.key", "minioadmin");

        // 🔴 CLÉ : éviter les fichiers temporaires sur disque => pas de NativeIO Windows
        configuration.set("fs.s3a.fast.upload", "true");
        configuration.set("fs.s3a.fast.upload.buffer", "array");
        // (valides: disk, array, bytebuffer — on force array = mémoire pure)
        // tu peux tuner un peu les tailles pour dev :
        configuration.set("fs.s3a.block.size", "8m");
        configuration.set("fs.s3a.multipart.size", "8m");
        configuration.set("fs.s3a.multipart.threshold", "8m");

        // Optionnel : répertoire tmp propre si jamais quelque chose en a besoin
        configuration.set("hadoop.tmp.dir", "C:/hadoop-tmp");

        //configuration.set("fs.defaultFS", "file:///");
        //configuration.setBoolean("fs.permissions.umask-mode.ignore", true);
        /*configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3a.aws.credentials.provider", "com.amazonaws.auth.DefaultAWSCredentialsProviderChain");
        configuration.setBoolean("fs.s3a.path.style.access", true);*/

        // 🔐 Forcer l'utilisation de LocalLogStore pour le schéma file:
        //configuration.set("spark.delta.logStore.file.impl", LocalLogStore.class.getName());
        return configuration;
    }

    private String resolveAssetCategory(DataImportJob job) {
        if (job == null) {
            log.info("[Delta] Unable to classify asset because job is null for symbol={}, defaulting to STOCK", job.getSymbol());
            return "STOCK";
        }
        String broker = Optional.ofNullable(job.getBroker()).orElse("").toUpperCase(Locale.ROOT);
        if (broker.contains("CME")) {
            return "CME";
        }

        String symbol = Optional.ofNullable(job.getSymbol()).orElse("").toUpperCase(Locale.ROOT);
        if (symbol.endsWith("USDT") || symbol.endsWith("USDC") || symbol.endsWith("BTC")) {
            return "CRYPTO";
        }

        String sourceType = Optional.ofNullable(job.getSourceType()).orElse("").toUpperCase(Locale.ROOT);
        if (sourceType.contains("ETF")) {
            return "ETF";
        }
        if (sourceType.contains("CFD") || symbol.contains("-CFD")) {
            return "CFD";
        }

        if (sourceType.contains("FUTURE") || Optional.ofNullable(job.getVenue()).orElse("").toUpperCase(Locale.ROOT).contains("CME")) {
            return "CME";
        }

        String assetClass = Optional.ofNullable(job.getAssetClass()).orElse("").toUpperCase(Locale.ROOT);
        if (assetClass.contains("FOREX") || assetClass.contains("FX")) {
            return "FOREX";
        }
        if (assetClass.contains("STOCK") || assetClass.contains("STK")) {
            return "STOCK";
        }

        log.info("[Delta] Unable to classify asset for symbol={}, defaulting to STOCK", job.getSymbol());
        return "STOCK";
    }

    private String resolveInsertedType(DataImportJob job, String assetCategory) {
        String normalizedCategory = defaultString(assetCategory).toUpperCase(Locale.ROOT);
        if (normalizedCategory.contains("CRYPTO")) {
            return "CRYPTO";
        }
        if (normalizedCategory.contains("ETF")) {
            return "ETF";
        }
        if (normalizedCategory.contains("FOREX") || normalizedCategory.equals("FX")) {
            return "FOREX";
        }
        String assetClass = defaultString(job.getAssetClass()).toUpperCase(Locale.ROOT);
        if (assetClass.contains("FOREX") || assetClass.equals("FX")) {
            return "FOREX";
        }
        if (assetClass.contains("ETF")) {
            return "ETF";
        }
        if (assetClass.contains("CRYPTO")) {
            return "CRYPTO";
        }
        return "STOCK";
    }

    private String normalizeInsertedType(String value) {
        String normalized = defaultString(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CRYPTO", "ETF", "FOREX", "STOCK" -> normalized;
            default -> "STOCK";
        };
    }
}
