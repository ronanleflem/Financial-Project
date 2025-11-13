package finance.project.api.dataimport;

import finance.project.api.dataimport.dto.DataImportRequest;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
        job.setVenue(req.venue());
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
}
