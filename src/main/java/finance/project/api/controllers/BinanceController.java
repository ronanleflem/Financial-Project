package finance.project.api.controllers;

import finance.project.api.services.BinanceFxService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/binance")
public class BinanceController extends AbstractFxController {

    public BinanceController(BinanceFxService binanceFxService) {
        super(binanceFxService);
    }
}
