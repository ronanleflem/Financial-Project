package finance.project.api.controllers;

import finance.project.api.model.CandleAvailabilityDTO;
import finance.project.api.services.CandleAvailabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance/charts")
public class CandleAvailabilityController {

    private final CandleAvailabilityService candleAvailabilityService;

    public CandleAvailabilityController(CandleAvailabilityService candleAvailabilityService) {
        this.candleAvailabilityService = candleAvailabilityService;
    }

    @GetMapping("/availability")
    public List<CandleAvailabilityDTO> getAvailability(@RequestParam(required = false) String symbol,
                                                       @RequestParam(required = false) String timeframe,
                                                       @RequestParam(required = false) String broker,
                                                       @RequestParam(required = false) String marketType) {
        return candleAvailabilityService.getAvailability(symbol, timeframe, broker, marketType);
    }
}
