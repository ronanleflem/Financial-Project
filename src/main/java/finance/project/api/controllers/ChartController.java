package finance.project.api.controllers;


import finance.project.api.model.ChartDataDTO;
import finance.project.api.services.ChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance/charts")
public class ChartController {

    private final ChartService chartService;


    @GetMapping
    public ResponseEntity<List<ChartDataDTO>> getChartData(@RequestParam String symbol, @RequestParam String interval) {
        List<ChartDataDTO> data = chartService.getChartData(symbol, interval);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @GetMapping("/default")
    public List<ChartDataDTO> listChartData(){
        return chartService.getChartData("AAPL", "daily");
    }
}
