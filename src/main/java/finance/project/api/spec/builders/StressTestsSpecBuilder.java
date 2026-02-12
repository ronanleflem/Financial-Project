package finance.project.api.spec.builders;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.PythonSpecBuilder;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Deprecated
@Component
@ConditionalOnProperty(name = "run.engine.mode", havingValue = "LEGACY")
public class StressTestsSpecBuilder implements PythonSpecBuilder {
    @Override
    public PythonSpec build(RunRequestInput input) {
        return new PythonSpec(input.specType(), Map.of());
    }

    @Override
    public String supportsSpecType() {
        return "stress_tests";
    }
}
