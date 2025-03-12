package finance.project.api.controllers;

import finance.project.api.entities.Candle;
import finance.project.api.services.VolumeBasedRolloverService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rollover-volume")
@RequiredArgsConstructor
public class VolumeBasedRolloverController {

    private final VolumeBasedRolloverService volumeBasedRolloverService;

    /**
     * Endpoint de récupération des candles avec rollover basé sur la dominance volume
     */
    @GetMapping("/unified-candles")
    public ResponseEntity<List<Candle>> getCandlesBasedOnVolume(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "2") int analysisPeriodDays // 2 jours par défaut
    ) {
        List<Candle> candles = volumeBasedRolloverService.getDynamicRolloverCandlesBasedOnVolume(startDate, endDate, analysisPeriodDays);
        return ResponseEntity.ok(candles);
    }
}
