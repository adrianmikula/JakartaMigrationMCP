package unit.jakartamigration.runtimeverification.service;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.runtimeverification.domain.*;
import com.bugbounty.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import com.bugbounty.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuntimeVerificationModule Tests")
class RuntimeVerificationModuleTest {
    
    private RuntimeVerificationModule module;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        module = new RuntimeVerificationModuleImpl();
    }
    
    @Test
    @DisplayName("Should verify runtime execution of JAR file")
    void shouldVerifyRuntimeExecution() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        Files.createFile(jarPath);
        
        VerificationOptions options = VerificationOptions.defaults();
        
        // When
        VerificationResult result = module.verifyRuntime(jarPath, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.status());
        assertNotNull(result.errors());
        assertNotNull(result.warnings());
        assertNotNull(result.metrics());
    }
    
    @Test
    @DisplayName("Should analyze errors for Jakarta migration issues")
    void shouldAnalyzeErrors() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of(new StackTrace.StackTraceElement(
                "com.example.Test",
                "testMethod",
                "Test.java",
                42
            ))
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertNotNull(analysis);
        assertNotNull(analysis.category());
        assertNotNull(analysis.rootCause());
        assertNotNull(analysis.suggestedFixes());
    }
    
    @Test
    @DisplayName("Should perform static analysis")
    void shouldPerformStaticAnalysis() throws Exception {
        // Given
        Path projectPath = tempDir;
        Files.createDirectories(projectPath);
        
        DependencyGraph graph = new DependencyGraph();
        
        // When
        StaticAnalysisResult result = module.performStaticAnalysis(projectPath, graph);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.potentialErrors());
        assertNotNull(result.warnings());
        assertNotNull(result.analyzedGraph());
        assertNotNull(result.analysisSummary());
    }
    
    @Test
    @DisplayName("Should instrument class loading")
    void shouldInstrumentClassLoading() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        Files.createFile(jarPath);
        
        InstrumentationOptions options = InstrumentationOptions.defaults();
        
        // When
        ClassLoaderAnalysisResult result = module.instrumentClassLoading(jarPath, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.loadedClasses());
        assertNotNull(result.failedLoads());
        assertNotNull(result.namespaceConflicts());
        assertNotNull(result.summary());
    }
    
    @Test
    @DisplayName("Should perform health check")
    void shouldPerformHealthCheck() {
        // Given
        String applicationUrl = "http://localhost:8080";
        HealthCheckOptions options = HealthCheckOptions.defaults();
        
        // When
        HealthCheckResult result = module.performHealthCheck(applicationUrl, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.responseTime());
        assertNotNull(result.issues());
    }
    
    @Test
    @DisplayName("Should detect namespace migration errors")
    void shouldDetectNamespaceMigrationErrors() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of()
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertEquals(ErrorCategory.NAMESPACE_MIGRATION, analysis.category());
        assertFalse(analysis.suggestedFixes().isEmpty());
    }
    
    @Test
    @DisplayName("Should detect classpath issues")
    void shouldDetectClasspathIssues() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.NoClassDefFoundError",
            "jakarta.servlet.ServletException",
            List.of()
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.NO_CLASS_DEF_FOUND,
            "jakarta.servlet.ServletException not found in classpath",
            stackTrace,
            "jakarta.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.9
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertEquals(ErrorCategory.CLASSPATH_ISSUE, analysis.category());
    }
}

