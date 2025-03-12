package finance.project.api.controllers;

import finance.project.api.entities.Candle;
import finance.project.api.services.RolloverCandleStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/rollover-stream")
@RequiredArgsConstructor
public class RolloverCandleStreamController {

    private final RolloverCandleStreamService rolloverCandleStreamService;

    @GetMapping("/unified-candles")
    public ResponseEntity<List<Candle>> getUnifiedCandles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<Candle> candles = rolloverCandleStreamService.getUnifiedCandles(startDate, endDate);
        return ResponseEntity.ok(candles);
    }
}
