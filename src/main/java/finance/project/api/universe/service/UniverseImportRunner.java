package finance.project.api.universe.service;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.DataImportJobRunner;
import finance.project.api.dataimport.DataImportService;
import finance.project.api.dataimport.dto.DataImportRequest;
import finance.project.api.entities.Symbol;
import finance.project.api.universe.Universe;
import finance.project.api.universe.UniverseRepository;
import finance.project.api.universe.dto.UniverseImportRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class UniverseImportRunner {

    private static final Logger log = LoggerFactory.getLogger(UniverseImportRunner.class);

    private final UniverseRepository universeRepository;
    private final DataImportService dataImportService;
    private final DataImportJobRunner dataImportJobRunner;

    public UniverseImportRunner(UniverseRepository universeRepository,
                                DataImportService dataImportService,
                                DataImportJobRunner dataImportJobRunner) {
        this.universeRepository = universeRepository;
        this.dataImportService = dataImportService;
        this.dataImportJobRunner = dataImportJobRunner;
    }

    @Async("dataImportExecutor")
    public void runUniverseImport(Long universeId, UniverseImportRequest request) {
        log.info("[UniverseImport] Starting import for universe={} timeframe={} broker={}", request.code(), request.timeframe(), request.broker());
        Universe universe = universeRepository.findWithSymbolsById(universeId)
                .orElse(null);
        if (universe == null) {
            log.warn("[UniverseImport] Universe {} not found, aborting import", universeId);
            return;
        }

        Set<Symbol> symbols = new LinkedHashSet<>(universe.getSymbols());
        log.info("[UniverseImport] Running {} jobs for universe {}", symbols.size(), universe.getCode());

        for (Symbol symbol : symbols) {
            try {
                DataImportRequest jobRequest = buildJobRequest(request, universe, symbol);
                DataImportJob job = dataImportService.createJob(jobRequest, false);
                dataImportJobRunner.runJobSync(job.getId());
            } catch (Exception e) {
                log.error("[UniverseImport] Failed job for symbol {}: {}", symbol.getSymbol(), e.getMessage(), e);
            }
            if (!pauseBetweenJobs()) {
                log.warn("[UniverseImport] Runner interrupted, stopping remaining jobs");
                break;
            }
        }
        log.info("[UniverseImport] Completed import for universe {}", universe.getCode());
    }

    private DataImportRequest buildJobRequest(UniverseImportRequest request, Universe universe, Symbol symbol) {
        String assetClass = request.assetClass() != null && !request.assetClass().isBlank()
                ? request.assetClass()
                : universe.getType().name();
        Instant start = request.startDate();
        Instant end = request.endDate();
        return new DataImportRequest(
                request.broker(),
                symbol.getSymbol(),
                request.timeframe(),
                start,
                end,
                "API",
                request.venue(),
                null,
                null,
                null,
                assetClass
        );
    }

    private boolean pauseBetweenJobs() {
        try {
            Thread.sleep(10_000L);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[UniverseImport] Runner interrupted while pausing between jobs");
            return false;
        }
    }
}
