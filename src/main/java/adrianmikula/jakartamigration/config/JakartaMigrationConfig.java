package adrianmikula.jakartamigration.config;

import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.coderefactoring.service.RefactoringEngine;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
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
     * WebClient for Apify API calls.
     * Configured with Apify API base URL and appropriate timeouts.
     */
    @Bean
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
     */
    @Bean
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
    public DependencyAnalysisModule dependencyAnalysisModule(
        DependencyGraphBuilder dependencyGraphBuilder,
        NamespaceClassifier namespaceClassifier
    ) {
        return new DependencyAnalysisModuleImpl(dependencyGraphBuilder, namespaceClassifier);
    }
    
    @Bean
    public MigrationPlanner migrationPlanner() {
        return new MigrationPlanner();
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
}

