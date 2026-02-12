package finance.project.api.services;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.PythonSpecBuilder;
import finance.project.api.spec.SpecBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Legacy Java spec-building service.
 * Target removal date: 2026-06-30.
 */
@Deprecated(forRemoval = true, since = "X-3")
@Service
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class PythonSpecService {
    private final SpecBuilderFactory builderFactory;

    public PythonSpecService(SpecBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    public PythonSpec buildSpec(RunRequestInput input) {
        PythonSpecBuilder builder = builderFactory.getBuilder(input.specType());
        return builder.build(input);
    }
}
