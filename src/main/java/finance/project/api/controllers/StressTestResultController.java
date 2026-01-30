package finance.project.api.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import finance.project.api.entities.StressTestResult;
import finance.project.api.model.StressTestRunSummaryDTO;
import finance.project.api.services.StressTestResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stress-tests")
public class StressTestResultController {

    private final StressTestResultService stressTestResultService;

    public StressTestResultController(StressTestResultService stressTestResultService) {
        this.stressTestResultService = stressTestResultService;
    }

    @GetMapping
    public ResponseEntity<List<StressTestResult>> getByRunId(@RequestParam String runId) {
        List<StressTestResult> results = stressTestResultService.getByRunId(runId);
        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StressTestResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stressTestResultService.getById(id));
    }

    @GetMapping("/summary")
    public ResponseEntity<ObjectNode> getSummary(@RequestParam String runId) {
        ObjectNode summary = stressTestResultService.buildSummary(runId);
        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/runs")
    public ResponseEntity<List<StressTestRunSummaryDTO>> listRuns() {
        List<StressTestRunSummaryDTO> runs = stressTestResultService.listRunSummaries();
        if (runs.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(runs);
    }
}
