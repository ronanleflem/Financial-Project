package finance.project.api.controllers;

import finance.project.api.services.IbkrFxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ibkr")
public class IbkrController extends AbstractFxController {

    private final IbkrFxService ibkrService;

    public IbkrController(IbkrFxService ibkrService) {
        super(ibkrService);
        this.ibkrService = ibkrService;
    }

    @GetMapping("/diag")
    public Map<String, Object> diag() {
        ensureConnected();
        return finance.project.api.bootstrap.IbkrRuntimeDiag.collect();
    }
}
