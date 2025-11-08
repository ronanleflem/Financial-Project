package finance.project.api.dataimport.api;

import finance.project.api.dataimport.DataImportJob;
import finance.project.api.dataimport.DataImportService;
import finance.project.api.dataimport.dto.DataImportJobResponse;
import finance.project.api.dataimport.dto.DataImportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-import/jobs")
public class DataImportJobController {

    private final DataImportService dataImportService;

    public DataImportJobController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @PostMapping
    public ResponseEntity<DataImportJobResponse> create(@RequestBody @Valid DataImportRequest request) {
        DataImportJob job = dataImportService.createJob(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(DataImportJobResponse.fromEntity(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataImportJobResponse> get(@PathVariable String id) {
        return dataImportService.getJob(id)
                .map(job -> ResponseEntity.ok(DataImportJobResponse.fromEntity(job)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
