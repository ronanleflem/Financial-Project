package finance.project.api.controllers;

import finance.project.api.services.CoinbaseFxService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coinbase")
public class CoinbaseController extends AbstractFxController {

    public CoinbaseController(CoinbaseFxService coinbaseFxService) {
        super(coinbaseFxService);
    }
}
