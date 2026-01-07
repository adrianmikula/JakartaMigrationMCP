package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine;
import adrianmikula.jakartamigration.coderefactoring.service.ChangeTracker;
import adrianmikula.jakartamigration.coderefactoring.service.ProgressTracker;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.JakartaMappingServiceImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
 * 
 * Component scanning is needed for test contexts that don't use the full Spring Boot application.
 */
@Configuration
@ComponentScan(basePackages = "adrianmikula.jakartamigration")
@EnableConfigurationProperties({
    FeatureFlagsProperties.class, 
    ApifyLicenseProperties.class,
    StripeLicenseProperties.class
})
public class JakartaMigrationConfig {
    
    /**
     * ApifyBillingService bean.
     * Handles billing events for premium features when deployed on Apify.
     * Only created if Apify validation is enabled.
     * 
     * NOTE: Apify support is deprecated in favor of Stripe.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "jakarta.migration.apify.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public ApifyBillingService apifyBillingService(ApifyLicenseProperties apifyProperties) {
        return new ApifyBillingService(apifyProperties);
    }

    /**
     * WebClient for Apify API calls.
     * Configured with Apify API base URL and appropriate timeouts.
     * Only created if Apify validation is enabled.
     * 
     * NOTE: Apify support is deprecated in favor of Stripe.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "jakarta.migration.apify.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public WebClient apifyWebClient(ApifyLicenseProperties apifyProperties) {
        return WebClient.builder()
            .baseUrl(apifyProperties.getApiUrl())
            .defaultHeader("Content-Type", "application/json")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(1024 * 1024)) // 1MB
            .build();
    }
    
    /**
     * WebClient for Stripe API calls.
     * Configured with Stripe API base URL and authentication.
     * Only created if Stripe validation is enabled.
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "jakarta.migration.stripe.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public WebClient stripeWebClient(StripeLicenseProperties stripeProperties) {
        return WebClient.builder()
            .baseUrl(stripeProperties.getApiUrl())
            .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(1024 * 1024)) // 1MB
            .build();
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
    public DependencyAnalysisModule dependencyAnalysisModule(
        DependencyGraphBuilder dependencyGraphBuilder,
        NamespaceClassifier namespaceClassifier,
        JakartaMappingService jakartaMappingService
    ) {
        return new DependencyAnalysisModuleImpl(dependencyGraphBuilder, namespaceClassifier, jakartaMappingService);
    }
    
    @Bean
    public MigrationPlanner migrationPlanner(adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner sourceCodeScanner) {
        return new MigrationPlanner(sourceCodeScanner);
    }
    
    @Bean
    public RecipeLibrary recipeLibrary() {
        return new RecipeLibrary();
    }
    
    @Bean
    public RuntimeVerificationModule runtimeVerificationModule() {
        return new RuntimeVerificationModuleImpl();
    }
    
    @Bean
    public RefactoringEngine refactoringEngine() {
        return new RefactoringEngine();
    }
    
    @Bean
    public ChangeTracker changeTracker() {
        return new ChangeTracker();
    }
    
    @Bean
    public ProgressTracker progressTracker() {
        return new ProgressTracker();
    }
}

