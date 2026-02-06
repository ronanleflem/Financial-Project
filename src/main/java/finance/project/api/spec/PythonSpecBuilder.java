package finance.project.api.spec;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;

public interface PythonSpecBuilder {
    PythonSpec build(RunRequestInput input);

    String supportsSpecType();
}
