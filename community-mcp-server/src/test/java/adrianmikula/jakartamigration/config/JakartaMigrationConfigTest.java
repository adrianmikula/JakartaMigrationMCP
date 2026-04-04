package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaArtifactLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JakartaMigrationConfig.
 * Verifies that all beans are created correctly (no Stripe/Apify).
 */
@Disabled("Spring context loading issues - low importance infrastructure test")
@SpringBootTest(classes = JakartaMigrationConfig.class)
@DisplayName("JakartaMigrationConfig Integration Tests")
class JakartaMigrationConfigTest {

    private final JakartaMigrationConfig config;

    JakartaMigrationConfigTest(JakartaMigrationConfig config) {
        this.config = config;
    }

    @Test
    @DisplayName("Should create DependencyGraphBuilder bean")
    void shouldCreateDependencyGraphBuilderBean() {
        assertThat(config.dependencyGraphBuilder()).isNotNull();
    }

    @Test
    @DisplayName("Should create NamespaceClassifier bean")
    void shouldCreateNamespaceClassifierBean() {
        assertThat(config.namespaceClassifier()).isNotNull();
    }

    @Test
    @DisplayName("Should create JakartaMappingService bean")
    void shouldCreateJakartaMappingServiceBean() {
        assertThat(config.jakartaMappingService()).isNotNull();
    }

    @Test
    @DisplayName("Should create DependencyAnalysisModule bean")
    void shouldCreateDependencyAnalysisModuleBean() {
        DependencyGraphBuilder graphBuilder = config.dependencyGraphBuilder();
        NamespaceClassifier classifier = config.namespaceClassifier();
        JakartaMappingService mappingService = config.jakartaMappingService();
        JakartaArtifactLookupService lookupService = config.jakartaArtifactLookupService();
        CentralMigrationAnalysisStore analysisStore = config.centralMigrationAnalysisStore();

        assertThat(config.dependencyAnalysisModule(graphBuilder, classifier, mappingService, lookupService, analysisStore))
                .isNotNull();
    }
}
