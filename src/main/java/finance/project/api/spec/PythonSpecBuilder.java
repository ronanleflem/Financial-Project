package finance.project.api.spec;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;

@Deprecated(forRemoval = true, since = "X-3")
public interface PythonSpecBuilder {
    PythonSpec build(RunRequestInput input);

    String supportsSpecType();
}
