package finance.project.api.performance.api;

import finance.project.api.performance.dto.PythonRunPayloadDTO;
import finance.project.api.performance.service.StrategyImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategies")
public class StrategyImportController {

    private final StrategyImportService strategyImportService;

    public StrategyImportController(StrategyImportService strategyImportService) {
        this.strategyImportService = strategyImportService;
    }

    @PostMapping("/import-run")
    public ResponseEntity<Void> importPythonRun(@RequestBody PythonRunPayloadDTO payload) {
        strategyImportService.importFromPython(payload);
        return ResponseEntity.ok().build();
    }
}
