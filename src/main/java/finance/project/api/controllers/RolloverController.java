package finance.project.api.controllers;

import finance.project.api.services.RolloverService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/rollover")
@RequiredArgsConstructor
public class RolloverController {

    private final RolloverService rolloverService;

    /**
     * Endpoint pour savoir quel est le contrat dominant à une date précise
     */
    @GetMapping("/dominant")
    public ResponseEntity<?> getDominantContract(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return rolloverService.getDominantContract(date)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint pour obtenir la date idéale de rollover sur un contrat donné
     */
    @GetMapping("/rollover-date")
    public ResponseEntity<?> getRolloverDate(@RequestParam String currentSymbol) {
        return rolloverService.getRolloverDateForSymbol(currentSymbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint pour obtenir le contrat suivant après le contrat actuel
     */
    @GetMapping("/next-contract")
    public ResponseEntity<?> getNextContract(@RequestParam String currentSymbol) {
        return rolloverService.getNextContractAfter(currentSymbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

