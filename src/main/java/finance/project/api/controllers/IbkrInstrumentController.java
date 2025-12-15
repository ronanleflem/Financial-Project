package finance.project.api.controllers;

import finance.project.api.ibkr.model.ResolvedInstrument;
import finance.project.api.services.IbkrFxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class IbkrInstrumentController {

    private final IbkrFxService ibkrFxService;

    public IbkrInstrumentController(IbkrFxService ibkrFxService) {
        this.ibkrFxService = ibkrFxService;
    }

    @GetMapping("/ibkr/instruments/resolve")
    public ResponseEntity<ResolvedInstrument> resolve(@RequestParam String symbol,
                                                      @RequestParam(required = false) String secType,
                                                      @RequestParam(required = false) String currency,
                                                      @RequestParam(required = false) String exchange) {
        ResolvedInstrument resolved = ibkrFxService.resolveContractMetadata(symbol, secType, exchange, currency, Duration.ofSeconds(5));
        return ResponseEntity.ok(resolved);
    }
}
