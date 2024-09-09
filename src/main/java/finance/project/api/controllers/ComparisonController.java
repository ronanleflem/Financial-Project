package finance.project.api.controllers;

import finance.project.api.model.ComparisonRequestDTO;
import finance.project.api.model.PerformanceComparisonDTO;
import finance.project.api.services.ComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;


    @PostMapping
    public ResponseEntity<PerformanceComparisonDTO> compareSymbols(@RequestBody ComparisonRequestDTO request) {
        PerformanceComparisonDTO result = comparisonService.comparePerformance(
                request.getSymbol1(), request.getSymbol2(), request.getStartDate(), request.getEndDate()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
