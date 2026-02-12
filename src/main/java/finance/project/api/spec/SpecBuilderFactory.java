package finance.project.api.spec;

@Deprecated
public interface SpecBuilderFactory {
    PythonSpecBuilder getBuilder(String specType);
}
