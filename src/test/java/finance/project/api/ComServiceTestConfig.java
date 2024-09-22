package finance.project.api;

import finance.project.api.bootstrap.BootstrapData;
import finance.project.api.repositories.CandleRepository;
import finance.project.api.repositories.ComparisonRepository;
import finance.project.api.repositories.SymbolRepository;
import finance.project.api.services.ComparisonService;
import finance.project.api.services.ComparisonServiceJPA;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@ComponentScan(basePackages = {"finance.project.api"})
public class ComServiceTestConfig {

    @MockBean
    private BootstrapData bootstrapData;
}
