package finance.project.api.controllers;

import finance.project.api.model.run.RunRequestInput;
import finance.project.api.model.PythonSpec;
import finance.project.api.model.ValidationErrorItem;
import finance.project.api.validation.RunRequestValidationException;
import finance.project.api.validation.RunRequestValidator;
import finance.project.api.services.PythonSpecService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RunController {
    private final RunRequestValidator runRequestValidator;
    private final PythonSpecService pythonSpecService;

    public RunController(RunRequestValidator runRequestValidator,
                         PythonSpecService pythonSpecService) {
        this.runRequestValidator = runRequestValidator;
        this.pythonSpecService = pythonSpecService;
    }

    @PostMapping("/runs")
    public ResponseEntity<Void> submit(@Valid @RequestBody RunRequestInput input) {
        validate(input);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/specs/preview")
    public ResponseEntity<PythonSpec> preview(@Valid @RequestBody RunRequestInput input) {
        validate(input);
        return ResponseEntity.ok(pythonSpecService.buildSpec(input));
    }

    private void validate(RunRequestInput input) {
        List<ValidationErrorItem> errors = runRequestValidator.validate(input);
        if (!errors.isEmpty()) {
            throw new RunRequestValidationException(errors);
        }
    }
}
