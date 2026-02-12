package finance.project.api.controllers;

import finance.project.api.model.ValidationErrorItem;
import finance.project.api.model.ValidationErrorResponse;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "PYTHON_CANONICAL", matchIfMissing = true)
public class CanonicalPreviewDisabledController {

    @PostMapping({"/specs/preview", "/runs/specs/preview"})
    public ResponseEntity<ValidationErrorResponse> previewUnavailableInCanonical() {
        ValidationErrorItem error = new ValidationErrorItem(
                "endpoint",
                "spec preview is deprecated in PYTHON_CANONICAL mode; submit /api/runs directly to Python"
        );
        return ResponseEntity.status(410)
                .body(new ValidationErrorResponse("LEGACY_PREVIEW_DISABLED", List.of(error)));
    }
}
