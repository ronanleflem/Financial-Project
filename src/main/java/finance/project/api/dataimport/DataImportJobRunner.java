package finance.project.api.dataimport;

import finance.project.api.dataimport.ingestion.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import finance.project.api.utils.DurationUtils;

@Service
public class DataImportJobRunner {

    private static final Logger log = LoggerFactory.getLogger(DataImportJobRunner.class);

    private final DataImportJobRepository jobRepository;
    private final BinanceHistoricalService binanceHistoricalService;
    private final MexcHistoricalService mexcHistoricalService;
    private final IbkrImportService ibkrImportService;
    private final DatabentoCsvImportService databentoCsvImportService;
    private final CsvImportService csvImportService;
    private final BitgetHistoricalService bitgetHistoricalService;

    public DataImportJobRunner(DataImportJobRepository jobRepository,
                               BinanceHistoricalService binanceHistoricalService,
                               MexcHistoricalService mexcHistoricalService,
                               IbkrImportService ibkrImportService,
                               DatabentoCsvImportService databentoCsvImportService,
                               CsvImportService csvImportService,
                               BitgetHistoricalService bitgetHistoricalService) {
        this.jobRepository = jobRepository;
        this.binanceHistoricalService = binanceHistoricalService;
        this.mexcHistoricalService = mexcHistoricalService;
        this.ibkrImportService = ibkrImportService;
        this.databentoCsvImportService = databentoCsvImportService;
        this.csvImportService = csvImportService;
        this.bitgetHistoricalService = bitgetHistoricalService;
    }

    @Async("dataImportExecutor")
    public void runJobAsync(String jobId) {
        runJob(jobId);
    }

    public void runJobSync(String jobId) {
        runJob(jobId);
    }

    private void runJob(String jobId) {
        log.info("[DataImport] Dispatching job {}", jobId);

        DataImportJob job = jobRepository.findById(jobId)
                .orElse(null);

        if (job == null) {
            log.error("[DataImport] Job {} not found", jobId);
            return;
        }

        try {
            // Passe en RUNNING
            job.setStatus(DataImportJob.Status.RUNNING);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);

            log.info("[DataImport] Job {} RUNNING for {} {} {} {}",
                    job.getId(), job.getBroker(), job.getSymbol(),
                    job.getTimeframe(), job.getSourceType());

            // Exécution réelle (switch broker/source)
            executeJob(job);

            // Succès
            job.setStatus(DataImportJob.Status.SUCCESS);
            job.setUpdatedAt(Instant.now());
            job.setMessage("Import completed");
            jobRepository.save(job);

            log.info("[DataImport] Job {} SUCCESS", job.getId());

        } catch (Exception e) {
            log.error("[DataImport] Job {} FAILED: {}", job.getId(), e.getMessage(), e);

            job.setStatus(DataImportJob.Status.FAILED);
            job.setUpdatedAt(Instant.now());
            job.setMessage(
                    Optional.ofNullable(e.getMessage()).orElse("Unexpected error in async import")
            );
            jobRepository.save(job);
        }
    }

    private void executeJob(DataImportJob job) {
        List<TimeRange> ranges;
        if (isCsvJob(job)) {
            ranges = new ArrayList<>();
            ranges.add(new TimeRange(job.getStartDate(), job.getEndDate()));
        } else {
            ranges = splitIntoChunks(job.getStartDate(), job.getEndDate(), job.getTimeframe());
            if (ranges.isEmpty()) {
                ranges.add(new TimeRange(job.getStartDate(), job.getEndDate()));
            }
        }

        int totalChunks = ranges.size();

        log.info("Executing job {} across {} chunk(s)", job.getId(), totalChunks);
        int processed = 0;
        for (TimeRange range : ranges) {
            processed++;
            updateMessage(job, String.format("Téléchargement %d/%d", processed, totalChunks));
            invokeBrokerImport(job, range);
            updateProgress(job, processed, totalChunks);
        }

        updateMessage(job, "Insertion en base terminée");
    }

    private void invokeCryptoWithFallback(DataImportJob job, Instant start, Instant end) {
        // 1) Essai Binance
        boolean ok = binanceHistoricalService.fetchAndSave(job, start, end);
        if (ok) {return;}

        log.info("[DataImport] Binance returned no data for job {}. Falling back to Bitget.", job.getId());

        // 2) Fallback Bitget
        job.setBroker("BITGET");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        ok = bitgetHistoricalService.fetchAndSave(job, start, end);
        if (ok) {
            log.info("[DataImport] Job {} successfully imported via Bitget", job.getId());
            return;
        }

        log.info("[DataImport] Bitget also returned no data for job {}. Falling back to MEXC.", job.getId());

        // 3) Fallback MEXC
        job.setBroker("MEXC");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        ok = mexcHistoricalService.fetchAndSave(job, start, end);
        if (ok) {
            log.info("[DataImport] Job {} successfully imported via MEXC", job.getId());
        } else {
            log.warn("[DataImport] No crypto provider (Binance/Bitget/MEXC) could supply data for job {}", job.getId());
        }
    }


    private void invokeBrokerImport(DataImportJob job, TimeRange range) {
        String broker = job.getBroker().toUpperCase();
        String sourceType = job.getSourceType().toUpperCase();
        Instant start = range.start();
        Instant end = range.end();

        switch (broker) {
            case "BINANCE" -> invokeCryptoWithFallback(job, start, end);
            case "MEXC" -> mexcHistoricalService.fetchAndSave(job, start, end);
            case "BITGET" -> bitgetHistoricalService.fetchAndSave(job, start, end);
            case "IBKR" -> ibkrImportService.fetchAndSave(job, start, end);
            case "DATABENTO_CSV" -> databentoCsvImportService.importCsv(
                    job, start, end, job.getVenue()
            );
            default -> {
                if ("CSV".equals(sourceType)) {
                    csvImportService.importGenericFile(job, start, end);
                } else {
                    log.warn("No dedicated handler for broker={} sourceType={} -> skipping chunk", broker, sourceType);
                }
            }
        }
    }

    private void transitionToRunning(DataImportJob job) {
        job.setStatus(DataImportJob.Status.RUNNING);
        job.setProgress(0);
        job.setMessage("Import en cours");
        jobRepository.save(job);
    }

    private void updateMessage(DataImportJob job, String message) {
        job.setMessage(message);
        jobRepository.save(job);
    }

    private void updateProgress(DataImportJob job, int processed, int totalChunks) {
        int progress = (int) Math.round(((double) processed / (double) totalChunks) * 90) + 10;
        job.setProgress(Math.min(99, Math.max(progress, 10)));
        jobRepository.save(job);
    }

    private void markSuccess(DataImportJob job) {
        job.setStatus(DataImportJob.Status.SUCCESS);
        job.setProgress(100);
        job.setMessage("Import terminé");
        jobRepository.save(job);
    }

    private void markFailure(DataImportJob job, Exception e) {
        job.setStatus(DataImportJob.Status.FAILED);
        job.setProgress(Math.min(job.getProgress(), 99));
        job.setMessage(truncate("Erreur : " + e.getMessage()));
        jobRepository.save(job);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 250 ? message.substring(0, 250) : message;
    }

    private boolean isCsvJob(DataImportJob job) {
        return job.getSourceType() != null && "CSV".equalsIgnoreCase(job.getSourceType());
    }

    private static final long MAX_CANDLES_PER_CHUNK = 200L;

    private List<TimeRange> splitIntoChunks(Instant start, Instant end, String timeframe) {
        List<TimeRange> ranges = new ArrayList<>();
        if (start == null || end == null || !start.isBefore(end)) {
            return ranges;
        }

        Duration total = Duration.between(start, end);
        Duration candleDuration = DurationUtils.parseTimeframe(timeframe);
        Duration maxChunkDuration = candleDuration.multipliedBy(MAX_CANDLES_PER_CHUNK);

        if (total.compareTo(maxChunkDuration) <= 0) {
            ranges.add(new TimeRange(start, end));
            return ranges;
        }

        Duration step = maxChunkDuration;
        Instant cursor = start;
        while (cursor.isBefore(end)) {
            Instant next = cursor.plus(step);
            if (next.isAfter(end)) {
                next = end;
            }
            ranges.add(new TimeRange(cursor, next));
            cursor = next;
        }
        return ranges;
    }

    private Duration determineChunkSize(Duration total) {
        if (total.compareTo(Duration.ofDays(180)) > 0) {
            return Duration.ofDays(30);
        }
        if (total.compareTo(Duration.ofDays(30)) > 0) {
            return Duration.ofDays(7);
        }
        if (total.compareTo(Duration.ofDays(7)) > 0) {
            return Duration.ofDays(1);
        }
        if (total.compareTo(Duration.ofHours(24)) > 0) {
            return Duration.ofHours(12);
        }
        if (total.compareTo(Duration.ofHours(6)) > 0) {
            return Duration.ofHours(6);
        }
        if (total.compareTo(Duration.ofHours(1)) > 0) {
            return Duration.ofHours(1);
        }
        return Duration.ofMinutes(15);
    }

    private record TimeRange(Instant start, Instant end) {
    }
}
