package finance.project.api.dataimport;

import finance.project.api.dataimport.dto.DataImportRequest;
import finance.project.api.entities.Symbol;
import finance.project.api.repositories.SymbolRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
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
    private final SymbolRepository symbolRepository;

    public DataImportService(DataImportJobRepository jobRepository,
                             DataImportJobRunner jobRunner,
                             SymbolRepository symbolRepository) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
        this.symbolRepository = symbolRepository;
    }


    public DataImportJob createJob(DataImportRequest req) {
        return createJob(req, true);
    }

    public DataImportJob createJob(DataImportRequest req, boolean startAsync) {
        Objects.requireNonNull(req, "DataImportRequest must not be null");

        if (!req.startDate().isBefore(req.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }

        ensureSymbolExists(req);

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

    private void ensureSymbolExists(DataImportRequest req) {
        String symbolCode = req.symbol();
        if (!StringUtils.hasText(symbolCode)) {
            return;
        }
        String trimmed = symbolCode.trim();
        boolean isEtf = isEtfAssetClass(req.assetClass());
        String normalized = (isEtf && isIsin(trimmed)) ? trimmed.toUpperCase(Locale.ROOT) : trimmed;

        Optional<Symbol> existing = Optional.empty();
        if (isEtf && isIsin(normalized)) {
            existing = symbolRepository.findByIsin(normalized);
        }
        if (existing.isEmpty()) {
            existing = symbolRepository.findBySymbol(normalized);
        }
        if (existing.isPresent()) {
            Symbol symbol = existing.get();
            boolean updated = false;
            if (isEtf && isIsin(normalized) && !StringUtils.hasText(symbol.getIsin())) {
                symbol.setIsin(normalized);
                updated = true;
            }
            if (updated) {
                symbolRepository.save(symbol);
            }
            return;
        }
        Symbol symbol = Symbol.builder()
                .symbol(normalized)
                .name(normalized)
                .market(resolveMarket(req))
                .exchange(resolveExchange(req))
                .currency(normalizeCurrency(req.currency()))
                .isin(isEtf && isIsin(normalized) ? normalized : null)
                .build();
        symbolRepository.save(symbol);
    }

    private String resolveMarket(DataImportRequest req) {
        if (StringUtils.hasText(req.assetClass())) {
            return req.assetClass().trim();
        }
        if (StringUtils.hasText(req.venue())) {
            return req.venue().trim();
        }
        return req.broker().trim();
    }

    private String resolveExchange(DataImportRequest req) {
        if (StringUtils.hasText(req.venue())) {
            return req.venue().trim();
        }
        return null;
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isEtfAssetClass(String assetClass) {
        return assetClass != null && assetClass.trim().equalsIgnoreCase("ETF");
    }

    private boolean isIsin(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.trim().toUpperCase(Locale.ROOT).matches("^[A-Z0-9]{12}$");
    }
}
