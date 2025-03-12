package finance.project.api.controllers;

import finance.project.api.entities.Symbology;
import finance.project.api.services.SymbologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/symbology")
@RequiredArgsConstructor
public class SymbologyController {

    private final SymbologyService symbologyService;

    /**
     * POST /symbology/load-json?filePath=csvData/eurusd/symbology/symbology.json
     * @param filePath
     * @return
     */
    @PostMapping("/load-json")
    public ResponseEntity<String> loadSymbology(@RequestParam String filePath) {
        symbologyService.loadSymbologyFromJson(filePath);
        return ResponseEntity.ok("Symbology JSON loaded successfully");
    }

    @GetMapping("/all")
    public ResponseEntity<List<Symbology>> getAllSymbology() {
        return ResponseEntity.ok(symbologyService.findAll());
    }

    /**
     * GET /symbology/by-symbol?symbol=6EM0
     * @param symbol
     * @return
     */
    @GetMapping("/by-symbol")
    public ResponseEntity<List<Symbology>> getBySymbol(@RequestParam String symbol) {
        return ResponseEntity.ok(symbologyService.findBySymbol(symbol));
    }
}
