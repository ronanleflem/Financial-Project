package finance.project.api.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import finance.project.api.model.PythonSpec;
import finance.project.api.model.run.RunRequestInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultSpecBuilderFactoryTest {

    @Test
    void returnsMatchingBuilder() {
        PythonSpecBuilder alpha = new StubBuilder("alpha");
        PythonSpecBuilder beta = new StubBuilder("beta");

        DefaultSpecBuilderFactory factory = new DefaultSpecBuilderFactory(List.of(alpha, beta));

        assertSame(alpha, factory.getBuilder("alpha"));
        assertSame(beta, factory.getBuilder("beta"));
    }

    @Test
    void throwsForUnknownSpecType() {
        DefaultSpecBuilderFactory factory = new DefaultSpecBuilderFactory(List.of(new StubBuilder("alpha")));

        InvalidSpecTypeException ex = assertThrows(InvalidSpecTypeException.class,
                () -> factory.getBuilder("unknown"));
        assertEquals("Unsupported specType: unknown", ex.getMessage());
    }

    private static final class StubBuilder implements PythonSpecBuilder {
        private final String specType;

        private StubBuilder(String specType) {
            this.specType = specType;
        }

        @Override
        public PythonSpec build(RunRequestInput input) {
            return new PythonSpec(specType, java.util.Map.of());
        }

        @Override
        public String supportsSpecType() {
            return specType;
        }
    }
}
