package finance.project.api.controllers;


import finance.project.api.model.CandleDTO;
import finance.project.api.services.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class CandleController {

    private final CandleService candleService;


    @GetMapping
    public ResponseEntity<List<CandleDTO>> getCandles(@RequestParam String symbol, @RequestParam String interval) {
        List<CandleDTO> data = candleService.getCandles(symbol, interval);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @GetMapping("/default")
    public List<CandleDTO> listCandles(){
        return candleService.getCandles("AAPL", "daily");
    }
}
