package com.bugbounty.jakartamigration.config;

import com.bugbounty.jakartamigration.coderefactoring.service.MigrationPlanner;
import com.bugbounty.jakartamigration.coderefactoring.service.RecipeLibrary;
import com.bugbounty.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import com.bugbounty.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import com.bugbounty.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import com.bugbounty.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import com.bugbounty.jakartamigration.dependencyanalysis.service.impl.MavenDependencyGraphBuilder;
import com.bugbounty.jakartamigration.dependencyanalysis.service.impl.SimpleNamespaceClassifier;
import com.bugbounty.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import com.bugbounty.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jakarta Migration modules.
 * Wires up all the service implementations.
 */
@Configuration
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
}

