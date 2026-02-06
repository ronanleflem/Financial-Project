package finance.project.api.spec;

public interface SpecBuilderFactory {
    PythonSpecBuilder getBuilder(String specType);
}
