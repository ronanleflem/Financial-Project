package finance.project.api.spec;

@Deprecated(forRemoval = true, since = "X-3")
public interface SpecBuilderFactory {
    PythonSpecBuilder getBuilder(String specType);
}
