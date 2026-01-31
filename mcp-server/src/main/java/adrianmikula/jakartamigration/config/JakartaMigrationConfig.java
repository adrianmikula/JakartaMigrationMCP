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

/**
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
 * 
 * Component scanning is needed for test contexts that don't use the full Spring Boot application.
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

