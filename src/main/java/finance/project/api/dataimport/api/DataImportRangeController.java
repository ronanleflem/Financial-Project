package finance.project.api.dataimport.api;

import finance.project.api.dataimport.dto.DeltaIngestionRangeResponse;
import finance.project.api.dataimport.infrastructure.DeltaIngestionRangeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-import/ranges")
public class DataImportRangeController {

    private final DeltaIngestionRangeService deltaIngestionRangeService;

    public DataImportRangeController(DeltaIngestionRangeService deltaIngestionRangeService) {
        this.deltaIngestionRangeService = deltaIngestionRangeService;
    }

    @GetMapping
    public ResponseEntity<List<DeltaIngestionRangeResponse>> listRanges(
            @RequestParam(value = "symbol", required = false) String symbol,
            @RequestParam(value = "insertedType", required = false) String insertedType,
            @RequestParam(value = "timeframe", required = false) String timeframe,
            @RequestParam(value = "limit", defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(
                deltaIngestionRangeService.findRanges(symbol, insertedType, timeframe, limit)
        );
    }
}

