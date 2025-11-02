package finance.project.api.controllers;

import finance.project.api.services.MexcFxService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mexc")
public class MexcController extends AbstractFxController {

    public MexcController(MexcFxService mexcFxService) {
        super(mexcFxService);
    }
}
