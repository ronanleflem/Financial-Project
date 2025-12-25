package finance.project.api.universe.api;

import finance.project.api.universe.dto.UniverseCatalogDTO;
import finance.project.api.universe.dto.UniverseDetailsDTO;
import finance.project.api.universe.dto.UniverseImportRequest;
import finance.project.api.universe.dto.UniverseImportResponse;
import finance.project.api.universe.service.UniverseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universes")
public class UniverseController {

    private final UniverseService universeService;

    public UniverseController(UniverseService universeService) {
        this.universeService = universeService;
    }

    @GetMapping("/catalog")
    public List<UniverseCatalogDTO> getCatalog() {
        return universeService.getCatalog();
    }

    @PostMapping("/import")
    public ResponseEntity<UniverseImportResponse> importUniverse(@RequestBody @Valid UniverseImportRequest request) {
        UniverseImportResponse response = universeService.importUniverse(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<UniverseDetailsDTO> getUniverse(@PathVariable String code) {
        return universeService.getUniverseDetails(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
