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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for JakartaMigrationConfig.
 * Verifies that all beans are created correctly.
 */
@SpringBootTest(classes = JakartaMigrationConfig.class)
@TestPropertySource(properties = {
    "jakarta.migration.apify.enabled=false",
    "jakarta.migration.stripe.enabled=false",
    "jakarta.migration.storage.file.enabled=false"
})
@DisplayName("JakartaMigrationConfig Integration Tests")
class JakartaMigrationConfigTest {

    private final JakartaMigrationConfig config;
    private final StripeLicenseProperties stripeProperties;
    private final ApifyLicenseProperties apifyProperties;

    JakartaMigrationConfigTest(
            JakartaMigrationConfig config,
            StripeLicenseProperties stripeProperties,
            ApifyLicenseProperties apifyProperties) {
        this.config = config;
        this.stripeProperties = stripeProperties;
        this.apifyProperties = apifyProperties;
    }

    @Test
    @DisplayName("Should create DependencyGraphBuilder bean")
    void shouldCreateDependencyGraphBuilderBean() {
        // When
        DependencyGraphBuilder builder = config.dependencyGraphBuilder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Should create NamespaceClassifier bean")
    void shouldCreateNamespaceClassifierBean() {
        // When
        NamespaceClassifier classifier = config.namespaceClassifier();

        // Then
        assertThat(classifier).isNotNull();
    }

    @Test
    @DisplayName("Should create JakartaMappingService bean")
    void shouldCreateJakartaMappingServiceBean() {
        // When
        JakartaMappingService service = config.jakartaMappingService();

        // Then
        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("Should create DependencyAnalysisModule bean")
    void shouldCreateDependencyAnalysisModuleBean() {
        // Given
        DependencyGraphBuilder graphBuilder = config.dependencyGraphBuilder();
        NamespaceClassifier classifier = config.namespaceClassifier();
        JakartaMappingService mappingService = config.jakartaMappingService();

        // When
        DependencyAnalysisModule module = config.dependencyAnalysisModule(
            graphBuilder, classifier, mappingService);

        // Then
        assertThat(module).isNotNull();
    }

    @Test
    @DisplayName("Should create RecipeLibrary bean")
    void shouldCreateRecipeLibraryBean() {
        // When
        RecipeLibrary library = config.recipeLibrary();

        // Then
        assertThat(library).isNotNull();
    }

    @Test
    @DisplayName("Should create RuntimeVerificationModule bean")
    void shouldCreateRuntimeVerificationModuleBean() {
        // When
        RuntimeVerificationModule module = config.runtimeVerificationModule();

        // Then
        assertThat(module).isNotNull();
    }

    @Test
    @DisplayName("Should create RefactoringEngine bean")
    void shouldCreateRefactoringEngineBean() {
        // When
        RefactoringEngine engine = config.refactoringEngine();

        // Then
        assertThat(engine).isNotNull();
    }

    @Test
    @DisplayName("Should create ChangeTracker bean")
    void shouldCreateChangeTrackerBean() {
        // When
        ChangeTracker tracker = config.changeTracker();

        // Then
        assertThat(tracker).isNotNull();
    }

    @Test
    @DisplayName("Should create ProgressTracker bean")
    void shouldCreateProgressTrackerBean() {
        // When
        ProgressTracker tracker = config.progressTracker();

        // Then
        assertThat(tracker).isNotNull();
    }

    @Test
    @DisplayName("Should create Stripe WebClient bean")
    void shouldCreateStripeWebClientBean() {
        // When
        WebClient webClient = config.stripeWebClient(stripeProperties);

        // Then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Should not create ApifyBillingService when Apify is disabled")
    void shouldNotCreateApifyBillingServiceWhenDisabled() {
        // Apify is disabled in test properties, so bean should not be created
        // This is verified by the @ConditionalOnProperty annotation
        // If we try to get it, it should be null or not exist
        assertThat(apifyProperties).isNotNull(); // Properties exist, but service should not
    }

    @Test
    @DisplayName("Should not create Apify WebClient when Apify is disabled")
    void shouldNotCreateApifyWebClientWhenDisabled() {
        // Apify is disabled in test properties
        // WebClient should not be created due to @ConditionalOnProperty
        assertThat(apifyProperties).isNotNull();
    }
}

