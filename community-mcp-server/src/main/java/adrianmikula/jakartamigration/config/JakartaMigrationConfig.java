package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaArtifactLookupService;
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
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
 *
 * Note: Recipe/refactoring beans removed - refactoring functionality
 * has been moved to premium-core-engine RecipeService (see REFACTOR.md).
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
    public JakartaArtifactLookupService jakartaArtifactLookupService() {
        return new JakartaArtifactLookupService();
    }

    @Bean
    public DependencyAnalysisModule dependencyAnalysisModule(
            DependencyGraphBuilder dependencyGraphBuilder,
            NamespaceClassifier namespaceClassifier,
            JakartaMappingService jakartaMappingService,
            JakartaArtifactLookupService jakartaArtifactLookupService) {
        return new DependencyAnalysisModuleImpl(dependencyGraphBuilder, namespaceClassifier, jakartaMappingService,
                jakartaArtifactLookupService);
    }
}
