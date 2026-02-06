package finance.project.api.services;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import finance.project.api.spec.PythonSpecBuilder;
import finance.project.api.spec.SpecBuilderFactory;
import org.springframework.stereotype.Service;

@Service
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
