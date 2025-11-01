package finance.project.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"finance.project.api", "com.yourapp.live"})
@ConfigurationPropertiesScan("com.yourapp.live")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
