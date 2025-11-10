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
import io.delta.standalone.types.DoubleType;
import io.delta.standalone.types.LongType;
import io.delta.standalone.types.StringType;
import io.delta.standalone.types.StructField;
import io.delta.standalone.types.StructType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
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

    private final DeltaLakeConfig deltaLakeConfig;

    public DeltaLakeExporter(DeltaLakeConfig deltaLakeConfig) {
        this.deltaLakeConfig = deltaLakeConfig;
    }

    public void exportCandlesToDelta(DataImportJob job, List<Candle> candles) {
        if (job == null) {
            log.warn("[Delta] DataImportJob is null, skipping export");
            return;
        }
        if (candles == null || candles.isEmpty()) {
            log.debug("[Delta] No candles to export for job {}", job.getId());
            return;
        }

        String assetCategory = resolveAssetCategory(job);
        String tablePath = deltaLakeConfig.resolveTablePath(assetCategory, job.getSymbol());
        String conflictPolicy = Optional.ofNullable(job.getConflictPolicy())
                .map(policy -> policy.toUpperCase(Locale.ROOT))
                .orElse("MERGE");

        Configuration conf = createHadoopConfiguration();
        try {
            DeltaLog deltaLog = DeltaLog.forTable(conf, tablePath);

            if ("SKIP".equals(conflictPolicy) && deltaLog.tableExists()) {
                log.info("[Delta] Conflict policy=SKIP and table already exists at {}. Skipping export.", tablePath);
                return;
            }

            OptimisticTransaction txn = deltaLog.startTransaction();

            if (!deltaLog.tableExists()) {
                Metadata metadata = Metadata.builder()
                        .schema(CANDLE_SCHEMA)
                        .name(job.getSymbol())
                        .description("Candles for " + job.getSymbol())
                        .build();
                txn.updateMetadata(metadata);
            }

            List<Action> actions = new ArrayList<>();
            if ("OVERWRITE".equals(conflictPolicy) && deltaLog.tableExists()) {
                deltaLog.snapshot().getAllFiles().stream()
                        .map(AddFile::remove)
                        .forEach(actions::add);
            }

            Path tableRoot = new Path(tablePath);
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
            parameters.put("mode", conflictPolicy);
            parameters.put("assetCategory", assetCategory);
            Operation operation = new Operation(Operation.Name.WRITE, parameters, Collections.emptyMap());

            txn.commit(actions, operation, job.getId());
            log.info("[Delta] Exported {} candles for job {} to {}", candles.size(), job.getId(), tablePath);
        } catch (Exception e) {
            log.error("[Delta] Failed to export candles for job {}: {}", job.getId(), e.getMessage(), e);
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
        configuration.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        configuration.set("fs.s3a.aws.credentials.provider", "com.amazonaws.auth.DefaultAWSCredentialsProviderChain");
        configuration.setBoolean("fs.s3a.path.style.access", true);
        return configuration;
    }

    private String resolveAssetCategory(DataImportJob job) {
        if (job == null) {
            return "ACTION";
        }
        if (job.getBroker() != null && job.getBroker().toUpperCase(Locale.ROOT).contains("CME")) {
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

        log.info("[Delta] Unable to classify asset for symbol={}, defaulting to ACTION", job.getSymbol());
        return "ACTION";
    }
}
