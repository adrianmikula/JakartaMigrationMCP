package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jakarta Migration (free build).
 * Wires only free-feature beans: dependency analysis, source code scanning, feature flags.
 * Pro features (Stripe, Apify, refactoring, runtime verification) are in the premium module.
 */
@Configuration
@ComponentScan(basePackages = "adrianmikula.jakartamigration")
@EnableConfigurationProperties(FeatureFlagsProperties.class)
public class JakartaMigrationConfig {

    @Bean
    public DependencyGraphBuilder dependencyGraphBuilder() {
        return new MavenDependencyGraphBuilder();
    }

    @Bean
    public NamespaceClassifier namespaceClassifier() {
        return new SimpleNamespaceClassifier();
    }

    @Bean
    public JakartaMappingService jakartaMappingService() {
        return new JakartaMappingServiceImpl();
    }

    @Bean
    public DependencyAnalysisModule dependencyAnalysisModule(
            DependencyGraphBuilder dependencyGraphBuilder,
            NamespaceClassifier namespaceClassifier,
            JakartaMappingService jakartaMappingService) {
        return new DependencyAnalysisModuleImpl(dependencyGraphBuilder, namespaceClassifier, jakartaMappingService);
    }
}
