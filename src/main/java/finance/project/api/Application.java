package finance.project.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EntityScan({"finance.project.api.entities", "finance.project.api.dataimport", "finance.project.api.live", "finance.project.api.universe"})
@EnableJpaRepositories({"finance.project.api.repositories", "finance.project.api.dataimport", "finance.project.api.live", "finance.project.api.universe"})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
