package finance.project.api.model.run;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class RunRequestInputTestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RunRequestInputTestSupport() {
    }

    static ObjectMapper mapper() {
        return MAPPER;
    }

    static String readFixture(String name) throws IOException {
        String path = "fixtures/run-request-input/" + name;
        try (InputStream stream = RunRequestInputTestSupport.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
