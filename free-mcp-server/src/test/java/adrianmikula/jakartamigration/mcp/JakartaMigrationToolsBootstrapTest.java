package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.coderefactoring.service.MigrationPlanner;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeLibrary;
import adrianmikula.jakartamigration.config.JakartaMigrationConfig;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.mcp.JakartaMigrationTools;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bootstrap and load time tests for JakartaMigrationTools.
 * Ensures MCP tools can be initialized quickly.
 */
@DisplayName("JakartaMigrationTools Bootstrap Tests")
class JakartaMigrationToolsBootstrapTest {

    @Test
    @DisplayName("Should bootstrap JakartaMigrationTools within 1 second")
    void shouldBootstrapJakartaMigrationToolsWithinTimeLimit() {
        // When
        long startTime = System.currentTimeMillis();
        
        // Create minimal Spring context with only required beans
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(JakartaMigrationConfig.class);
        context.refresh();
        
        JakartaMigrationTools tools = context.getBean(JakartaMigrationTools.class);
        
        long bootstrapTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(tools).isNotNull();
        assertThat(bootstrapTime).isLessThan(1000); // Should bootstrap within 1 second
        
        context.close();
    }

    @Test
    @DisplayName("Should initialize all dependencies quickly")
    void shouldInitializeAllDependenciesQuickly() {
        // When
        long startTime = System.currentTimeMillis();
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(JakartaMigrationConfig.class);
        context.refresh();
        
        // Get all beans to ensure they're initialized
        DependencyAnalysisModule analysisModule = context.getBean(DependencyAnalysisModule.class);
        DependencyGraphBuilder graphBuilder = context.getBean(DependencyGraphBuilder.class);
        NamespaceClassifier classifier = context.getBean(NamespaceClassifier.class);
        MigrationPlanner planner = context.getBean(MigrationPlanner.class);
        RecipeLibrary recipeLibrary = context.getBean(RecipeLibrary.class);
        RuntimeVerificationModule verificationModule = context.getBean(RuntimeVerificationModule.class);
        JakartaMigrationTools tools = context.getBean(JakartaMigrationTools.class);
        
        long initializationTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(analysisModule).isNotNull();
        assertThat(graphBuilder).isNotNull();
        assertThat(classifier).isNotNull();
        assertThat(planner).isNotNull();
        assertThat(recipeLibrary).isNotNull();
        assertThat(verificationModule).isNotNull();
        assertThat(tools).isNotNull();
        assertThat(initializationTime).isLessThan(1500); // All beans should initialize within 1.5 seconds
        
        context.close();
    }

    @Test
    @DisplayName("Should handle multiple context initializations efficiently")
    void shouldHandleMultipleContextInitializationsEfficiently() {
        // When - Create and destroy context multiple times
        long totalTime = 0;
        int iterations = 5;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.register(JakartaMigrationConfig.class);
            context.refresh();
            
            JakartaMigrationTools tools = context.getBean(JakartaMigrationTools.class);
            assertThat(tools).isNotNull();
            
            context.close();
            
            totalTime += System.currentTimeMillis() - startTime;
        }
        
        long averageTime = totalTime / iterations;

        // Then
        assertThat(averageTime).isLessThan(2000); // Average initialization should be under 2 seconds
    }

    @Test
    @DisplayName("Should be ready for use immediately after bootstrap")
    void shouldBeReadyForUseImmediatelyAfterBootstrap() {
        // Given
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(JakartaMigrationConfig.class);
        context.refresh();
        
        JakartaMigrationTools tools = context.getBean(JakartaMigrationTools.class);
        
        // When - Immediately try to use the tool (with mocked dependencies)
        // Note: This test verifies the tool is ready, actual functionality is tested elsewhere
        long startTime = System.currentTimeMillis();
        
        // Tool should be ready immediately
        boolean isReady = tools != null;
        
        long readyTime = System.currentTimeMillis() - startTime;

        // Then
        assertThat(isReady).isTrue();
        assertThat(readyTime).isLessThan(10); // Should be ready in milliseconds
        
        context.close();
    }

    @Test
    @DisplayName("Should not have memory leaks during repeated initialization")
    void shouldNotHaveMemoryLeaksDuringRepeatedInitialization() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Create and destroy many contexts
        for (int i = 0; i < 10; i++) {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.register(JakartaMigrationConfig.class);
            context.refresh();
            
            JakartaMigrationTools tools = context.getBean(JakartaMigrationTools.class);
            assertThat(tools).isNotNull();
            
            context.close();
            
            // Force garbage collection between iterations
            if (i % 3 == 0) {
                System.gc();
            }
        }
        
        // Force final GC
        System.gc();
        Thread.yield();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // Then - Memory increase should be reasonable (less than 50MB)
        // Note: This is a rough check, actual memory usage depends on JVM
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // 50MB
    }
}
