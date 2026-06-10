package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
 *
 * Note: Recipe/refactoring beans removed - refactoring functionality
 * has been moved to premium-core-engine RecipeService (see REFACTOR.md).
 */
@Configuration
@ComponentScan(basePackages = "adrianmikula.jakartamigration")
public class JakartaMigrationConfig {

    /**
     * Keep-alive for stdio mode.
     * Spring AI MCP stdio transport may use a daemon thread for reading stdin,
     * which doesn't prevent JVM exit. This bean spawns a non-daemon thread
     * that blocks indefinitely while stdio transport is active.
     */
    @Bean
    public KeepAlive keepAlive(Environment env) {
        String transport = env.getProperty("spring.ai.mcp.server.transport", "stdio");
        return new KeepAlive("stdio".equalsIgnoreCase(transport));
    }

    @Bean
    public FeatureFlagsProperties featureFlagsProperties() {
        return new FeatureFlagsProperties();
    }

    @Bean
    public LicenseService licenseService(FeatureFlagsProperties properties) {
        return new LicenseService(properties);
    }

    @Bean
    public FeatureFlagsService featureFlagsService(FeatureFlagsProperties properties, LicenseService licenseService) {
        return new FeatureFlagsService(properties, licenseService);
    }

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
    public ImprovedMavenCentralLookupService jakartaArtifactLookupService() {
        return new ImprovedMavenCentralLookupService();
    }

    @Bean
    public CentralMigrationAnalysisStore centralMigrationAnalysisStore() {
        return new CentralMigrationAnalysisStore();
    }

    @Bean
    public DependencyAnalysisModule dependencyAnalysisModule(
            DependencyGraphBuilder dependencyGraphBuilder,
            NamespaceClassifier namespaceClassifier,
            JakartaMappingService jakartaMappingService,
            ImprovedMavenCentralLookupService jakartaArtifactLookupService,
            CentralMigrationAnalysisStore analysisStore) {
        return new DependencyAnalysisModuleImpl(dependencyGraphBuilder, namespaceClassifier, jakartaMappingService,
                jakartaArtifactLookupService, analysisStore);
    }
}
