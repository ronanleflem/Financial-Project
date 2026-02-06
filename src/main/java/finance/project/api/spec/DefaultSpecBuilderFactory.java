package finance.project.api.spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultSpecBuilderFactory implements SpecBuilderFactory {

    private final Map<String, PythonSpecBuilder> builders;

    public DefaultSpecBuilderFactory(List<PythonSpecBuilder> builderList) {
        this.builders = builderList.stream()
                .collect(Collectors.toMap(PythonSpecBuilder::supportsSpecType, builder -> builder));
    }

    @Override
    public PythonSpecBuilder getBuilder(String specType) {
        PythonSpecBuilder builder = builders.get(specType);
        if (builder == null) {
            throw new InvalidSpecTypeException(specType);
        }
        return builder;
    }
}
