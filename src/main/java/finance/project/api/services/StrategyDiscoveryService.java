package finance.project.api.services;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility service that scans the classpath for strategy classes located in the
 * "finance.project.api.strategies.ta4j" package. It returns their simple class
 * names so they can be exposed through an API endpoint.
 */
@Service
public class StrategyDiscoveryService {
    private static final String STRATEGIES_PACKAGE = "finance.project.api.strategies.ta4j";

    /**
     * Scans the predefined strategies package and returns all strategy class names.
     *
     * @return list of available strategy names
     */
    public List<String> getAvailableStrategies() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        Set<BeanDefinition> components = scanner.findCandidateComponents(STRATEGIES_PACKAGE);
        List<String> names = new ArrayList<>();
        for (BeanDefinition bd : components) {
            String className = bd.getBeanClassName();
            if (className != null) {
                int idx = className.lastIndexOf('.');
                names.add(className.substring(idx + 1));
            }
        }
        return names;
    }
}
