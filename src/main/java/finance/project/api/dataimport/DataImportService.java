package finance.project.api.dataimport;

import finance.project.api.dataimport.dto.DataImportRequest;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataImportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportService.class);

    private final DataImportJobRepository jobRepository;
    private final DataImportJobRunner jobRunner;

    public DataImportService(DataImportJobRepository jobRepository, DataImportJobRunner jobRunner) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
    }


    public DataImportJob createJob(DataImportRequest req) {
        return createJob(req, true);
    }

    public DataImportJob createJob(DataImportRequest req, boolean startAsync) {
        Objects.requireNonNull(req, "DataImportRequest must not be null");

        if (!req.startDate().isBefore(req.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        DataImportJob job = new DataImportJob();
        job.setId(UUID.randomUUID().toString());
        job.setBroker(req.broker());
        job.setSymbol(req.symbol());
        job.setTimeframe(req.timeframe());
        job.setAssetClass(req.assetClass());
        job.setVenue(resolveVenue(req));
        job.setCurrency(req.currency());
        job.setTimezone(req.timezone());
        job.setConflictPolicy(req.conflictPolicy());
        job.setRollover(req.rollover());
        job.setStartDate(req.startDate());
        job.setEndDate(req.endDate());
        job.setSourceType(req.sourceType());
        job.setStatus(DataImportJob.Status.PENDING);
        job.setProgress(0);
        job.setMessage(null);

        log.info("Creating data import job {} for broker={} symbol={} timeframe={}", job.getId(), job.getBroker(), job.getSymbol(), job.getTimeframe());
        jobRepository.save(job);

        if (startAsync) {
            jobRunner.runJobAsync(job.getId());
        }
        return job;
    }

    public Optional<DataImportJob> getJob(String id) {
        return jobRepository.findById(id);
    }

    private String resolveVenue(DataImportRequest req) {
        if (StringUtils.hasText(req.venue())) {
            return req.venue();
        }

        String assetClass = req.assetClass();
        if (assetClass != null && assetClass.toUpperCase().contains("CRYPTO")) {
            return "SPOT";
        }

        String broker = req.broker();
        if (broker != null && isCryptoBroker(broker)) {
            return "SPOT";
        }

        return req.venue();
    }

    private boolean isCryptoBroker(String broker) {
        String normalized = broker.trim().toUpperCase();
        return normalized.equals("BINANCE")
                || normalized.equals("OKX")
                || normalized.equals("BYBIT")
                || normalized.equals("MEXC")
                || normalized.equals("BITGET");
    }
}
