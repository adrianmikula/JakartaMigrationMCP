package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.coderefactoring.service.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JakartaMigrationConfig.
 * Verifies that all beans are created correctly (no Stripe/Apify).
 */
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

        assertThat(config.dependencyAnalysisModule(graphBuilder, classifier, mappingService)).isNotNull();
    }

    @Test
    @DisplayName("Should create RecipeLibrary bean")
    void shouldCreateRecipeLibraryBean() {
        assertThat(config.recipeLibrary()).isNotNull();
    }

    @Test
    @DisplayName("Should create RuntimeVerificationModule bean")
    void shouldCreateRuntimeVerificationModuleBean() {
        assertThat(config.runtimeVerificationModule()).isNotNull();
    }

    @Test
    @DisplayName("Should create RefactoringEngine bean")
    void shouldCreateRefactoringEngineBean() {
        assertThat(config.refactoringEngine()).isNotNull();
    }

    @Test
    @DisplayName("Should create ChangeTracker bean")
    void shouldCreateChangeTrackerBean() {
        assertThat(config.changeTracker()).isNotNull();
    }

    @Test
    @DisplayName("Should create ProgressTracker bean")
    void shouldCreateProgressTrackerBean() {
        assertThat(config.progressTracker()).isNotNull();
    }
}
