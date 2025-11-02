package finance.project.api.controllers;

import finance.project.api.services.BitgetFxService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bitget")
public class BitgetController extends AbstractFxController {

    public BitgetController(BitgetFxService bitgetFxService) {
        super(bitgetFxService);
    }
}
