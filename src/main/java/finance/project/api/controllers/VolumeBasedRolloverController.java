package finance.project.api.controllers;

import finance.project.api.entities.Candle;
import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleAggregationService;
import finance.project.api.services.VolumeBasedRolloverNewService;
import finance.project.api.services.VolumeBasedRolloverService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rollover-volume")
@RequiredArgsConstructor
public class VolumeBasedRolloverController {

    private final VolumeBasedRolloverService volumeBasedRolloverService;
    private final CandleAggregationService candleAggregationService;

    /**
     * Endpoint de récupération des candles avec rollover basé sur la dominance volume
     */
    /*
    @GetMapping("/unified-candles")
    public ResponseEntity<List<CandleDTO>> getCandlesBasedOnVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "2") int analysisPeriodDays,
            @RequestParam(defaultValue = "M1") String timeframe
    ) {
        List<CandleDTO> candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolumeIndexed(startDate, endDate, analysisPeriodDays);
        List<CandleDTO> aggregatedCandles = candleAggregationService.aggregateCandles(candles, timeframe);
        return ResponseEntity.ok(aggregatedCandles);
    }*/

    @GetMapping("/heatmap-dominance")
    public ResponseEntity<Map<LocalDateTime, String>> getHeatmapDominance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestParam(defaultValue = "2") int analysisPeriodDays
    ) {
        Map<LocalDateTime, String> heatmap = volumeBasedRolloverService.getDominantContractsPerDay(
                startDate, endDate, analysisPeriodDays
        );

        return ResponseEntity.ok(heatmap);
    }

}
